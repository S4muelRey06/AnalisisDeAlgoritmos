package com.health.project.horarios;

import com.health.project.entitys.*;
import com.health.project.repository.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Controller
public class HorarioController {
    private final EstudianteRepository estudiantes;
    private final MateriaRepository materias;
    private final ProfesorRepository profesores;
    private final ClaseRepository clases;
    private final ObjectMapper json;

    public HorarioController(EstudianteRepository estudiantes, MateriaRepository materias, ProfesorRepository profesores,
                             ClaseRepository clases, ObjectMapper json) {
        this.estudiantes = estudiantes;
        this.materias = materias;
        this.profesores = profesores;
        this.clases = clases;
        this.json = json;
    }

    @GetMapping("/")
    String inicio(Model model) {
        model.addAttribute("estudiantes", estudiantes.findAll());
        return "index";
    }

    @GetMapping("/estudiantes/{id}/horario")
    String formulario(@PathVariable long id, Model model) {
        Estudiante estudiante = estudiante(id);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("materias", materias.findAll());
        model.addAttribute("profesores", profesores.findAll());
        return "solicitud-horario";
    }

    @GetMapping(value="/estudiantes/{id}/horario/guardado", produces="application/json")
    @ResponseBody
    String guardado(@PathVariable long id) {
        String horario = estudiante(id).getHorario();
        if (horario == null || horario.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Sin horario guardado");
        return horario;
    }

    private Estudiante estudiante(long id) {
        return estudiantes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping(value="/estudiantes/{id}/horario", consumes="application/json", produces="application/json")
    @ResponseBody
    @Transactional
    GeneradorHorario.Resultado desdeJson(@PathVariable long id, @RequestBody SolicitudHorario solicitud) {
        var campos = camposSolicitud(solicitud);
        var model = new org.springframework.ui.ExtendedModelMap();
        generar(id,campos,model);
        if (model.containsAttribute("error")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solicitud inválida");
        return (GeneradorHorario.Resultado) model.get("resultado");
    }
    private MultiValueMap<String,String> camposSolicitud(SolicitudHorario solicitud) {
        if (solicitud.materias() == null || solicitud.detalles() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Faltan materias o detalles");
        var campos = new org.springframework.util.LinkedMultiValueMap<String,String>();
        var catalogo = materias.findAll();
        solicitud.materias().forEach((nombre, valor) -> {
            var materia = catalogo.stream().filter(m -> m.getNombre().equalsIgnoreCase(nombre)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Materia desconocida"));
            String clave = materia.getId().toString();
            campos.add("materia",clave);
            String prioridad = Objects.toString(valor,"").replaceFirst("(?i)^prioridad +","");
            campos.add("prioridad_"+clave,List.of("Alta","Media","Baja").stream()
                .filter(p -> p.equalsIgnoreCase(prioridad)).findFirst().orElse(""));
        });
        if (solicitud.profesores() != null) profesores.findAll().stream()
            .filter(p -> solicitud.profesores().contains(p.getNombre()))
            .forEach(p -> campos.add("profesor",p.getId().toString()));
        var detalles = solicitud.detalles();
        if (detalles.franjas_horarias()!=null) campos.addAll("franja",detalles.franjas_horarias());
        campos.add("cercania",Objects.toString(detalles.cercania(),"0"));
        campos.add("volumen",Objects.toString(detalles.volumen_de_compañeros(),"libre"));
        campos.add("promedio",String.valueOf(detalles.promedio()));
        return campos;
    }

    @PostMapping(value="/estudiantes/{id}/horario", consumes="application/x-www-form-urlencoded")
    @Transactional
    String generar(@PathVariable long id, @RequestParam MultiValueMap<String,String> campos, Model model) {
        Estudiante estudiante = estudiantes.bloquear(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Objects.toString(campos.getFirst("solicitud_json"),"").isBlank()) {
            var solicitud = json.readValue(campos.getFirst("solicitud_json"), SolicitudHorario.class);
            campos = camposSolicitud(solicitud);
        }
        List<String> prioridades = List.of("Alta","Media","Baja");
        Map<Materia,Integer> solicitadas = new LinkedHashMap<>();
        GeneradorHorario.Preferencias preferencias;
        try {
            for (String clave : campos.getOrDefault("materia", List.of())) {
                Materia materia = materias.findById(Long.parseLong(clave)).orElseThrow(IllegalArgumentException::new);
                int prioridad = prioridades.indexOf(campos.getFirst("prioridad_"+clave));
                if (prioridad < 0 || solicitadas.put(materia,prioridad) != null) throw new IllegalArgumentException();
            }
            List<String> franjas = campos.getOrDefault("franja",List.of());
            List<Long> docentes = campos.getOrDefault("profesor",List.of()).stream().map(Long::valueOf).toList();
            int cercania = Integer.parseInt(campos.getFirst("cercania"));
            double promedio = Double.parseDouble(campos.getFirst("promedio"));
            String volumen = campos.getFirst("volumen");
            if (solicitadas.isEmpty() || cercania < 0 || cercania > 2 || !Double.isFinite(promedio)
                || promedio < 0 || promedio > 5 || !List.of("libre","alto","bajo").contains(volumen)
                || !List.of("mañana","tarde","noche").containsAll(franjas)) throw new IllegalArgumentException();
            preferencias = new GeneradorHorario.Preferencias(franjas,volumen,docentes,cercania,promedio);
        } catch (IllegalArgumentException | NullPointerException ex) {
            model.addAttribute("error","Selecciona al menos una materia y revisa las preferencias.");
            model.addAttribute("campos",campos);
            return formulario(id,model);
        }
        var ordenadas = solicitadas.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        var resultado = new GeneradorHorario().generar(ordenadas,clases.findAll(),preferencias, grupos(estudiante.getHorarioConfirmado()));
        estudiante.setPromedio(preferencias.promedio());
        estudiante.setHorario(json.writeValueAsString(resultado));
        model.addAttribute("estudiante",estudiante);
        model.addAttribute("resultado",resultado);
        model.addAttribute("firma", firma(estudiante));
        model.addAttribute("confirmable", resultado.horario().values().stream().anyMatch(d -> !d.isEmpty()));
        return "resultado-horario";
    }

    private String firma(Estudiante estudiante) {
        String propuesta = estudiante.getId()+"|"+estudiante.getPromedio()+"|"+estudiante.getHorario();
        return UUID.nameUUIDFromBytes(propuesta.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private Set<Long> grupos(String horario) {
        Set<Long> grupos = new TreeSet<>();
        if (horario != null) {
            for (var dia : json.readTree(horario).get("horario").properties())
                for (var sesion : dia.getValue()) grupos.add(sesion.get("numero_de_clase").asLong());
        }
        return grupos;
    }

    @PostMapping("/estudiantes/{id}/horario/confirmar")
    @Transactional
    String confirmar(@PathVariable long id, @RequestParam String firma, Model model) {
        var estudiante = estudiantes.bloquear(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!firma(estudiante).equals(firma))
            return mensaje(model,"La propuesta cambió. Genera y revisa tu horario antes de confirmarlo.");
        var nuevos = grupos(estudiante.getHorario());
        if (nuevos.isEmpty()) return mensaje(model,"No hay grupos para confirmar.");
        var anteriores = grupos(estudiante.getHorarioConfirmado());
        var ids = new TreeSet<>(anteriores);
        ids.addAll(nuevos);
        // Bloquear siempre en el mismo orden y validar todo antes de descontar o reservar cupos.
        List<Clase> bloqueadas = new ArrayList<>();
        for (Long grupo : ids) {
            var clase = clases.bloquear(grupo).orElse(null);
            if (clase == null) return mensaje(model,"Un grupo ya no existe. Genera otra propuesta.");
            if (!anteriores.contains(grupo) && clase.getCuposOcupados() >= clase.getCupos())
                return mensaje(model,"El grupo "+grupo+" se quedó sin cupos. Genera otra propuesta.");
            bloqueadas.add(clase);
        }
        for (Clase clase : bloqueadas) {
            boolean antes = anteriores.contains(clase.getId()), ahora = nuevos.contains(clase.getId());
            int cambio = (ahora ? 1 : 0) - (antes ? 1 : 0);
            double aporte = (ahora ? estudiante.getPromedio() : 0) - (antes ? estudiante.getPromedioConfirmado() : 0);
            clase.setCuposOcupados(clase.getCuposOcupados()+cambio);
            clase.setSumaPromedios(clase.getSumaPromedios()+aporte);
        }
        estudiante.setHorarioConfirmado(estudiante.getHorario());
        estudiante.setPromedioConfirmado(estudiante.getPromedio());
        return mensaje(model,"Horario confirmado. Tus cupos quedaron registrados.");
    }

    private String mensaje(Model model, String texto) {
        model.addAttribute("mensaje",texto);
        return "confirmacion-horario";
    }
}
