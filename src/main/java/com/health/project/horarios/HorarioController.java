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


//RECIBE LAS SOLICITUDES, GENERA LAS PROPUESTAS Y CREA LOS HORARIOS
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

    
    //CARGA LOS ESTUDIANTES EN LA PÁGINA INICIAL, LOS TRAE DESDE BD
    @GetMapping("/")
    String inicio(Model model) {
        model.addAttribute("estudiantes", estudiantes.findAll());
        return "index";
    }

    //MUESTRA EL FORMULARIO PARA CADA ESTUDIANTE, DESPLIEGA LA PANTALLA PRINCIPAL CON EL ID DE ESTUDIANTE
    @GetMapping("/estudiantes/{id}/horario")
    String formulario(@PathVariable long id, Model model) {
        Estudiante estudiante = estudiante(id);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("materias", materias.findAll());
        model.addAttribute("profesores", profesores.findAll());
        return "solicitud-horario";
    }

    //TRAE EL HORARIO DEL ESTUDIANTE QUE ESTÁ ARMADO DE MOMENTO EN LA BD, QUE ES DE TIPO JSON
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

    //OJO AQUI
    //1. AQUI SE RECIBE UN OBJETO "SolicitudHorario"
    //2. Se llama a camposSolicitud() para transformar los datos de 
    //3. Se crea un ExtendedModelMap, que es un objeto de Springboot y funciona como un mapa
    //3. Se llama al método generar de "Planificador" enviando el objeto anterior como parámetro, ahí ocurre el proceso de programación dinámica
    //4. Devuelve un objeto de tipo resultado, que contiene las salidas (se puede revisar en generadorHorario)
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

    //Recibe la información del formulario, es decir las materias con sus prioridades y las otras preferencias
    //Valida que la materia exista en BD
    //Convierte los nombres de las materias en id, igual que los profesores y niveles de prioridad
    //Agrega a los detalles el resto de preferencias
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

    // ESTE ES EL MÁS IMPORTANTE DE ESTE ARCHIVO:
    // BLOQUEA LA SELECCION DEL ESTUDIANTE
    // ASIGNA NUMEROS A LAS PRIORIDADES 0=ALTA 1=MEDIA 2=BAJA Y LOS ENLAZA A LAS MATERIAS
    // ORDENA LAS MATERIAS POR PRIORIDAD (NIVEL 1 DEL ALGORITMO - EL RESTO DE NIVELES ESTÁN EN GENERADORHORARIO)
    // HACE EL MÉTODO GENERAR DE GENERADOR HORARIO QUE ES EL CENTRO DEL ALGORITMO
    // EL HORARIO PROPUESTO SE SERIALIZA A JSON Y SE GUARDA EN EL ESTUDIANTE
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

    
    // ESTE GENERA UN IDENTIFICADOR PARA LA VERSIÓN DEL HORARIO, PARA DISTINGUIR LAS PROPUESTAS
    private String firma(Estudiante estudiante) {
        String propuesta = estudiante.getId()+"|"+estudiante.getPromedio()+"|"+estudiante.getHorario();
        return UUID.nameUUIDFromBytes(propuesta.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    // Extrae los identificadorees de las clases metidas en una propuesta de horarios
    private Set<Long> grupos(String horario) {
        Set<Long> grupos = new TreeSet<>();
        if (horario != null) {
            for (var dia : json.readTree(horario).get("horario").properties())
                for (var sesion : dia.getValue()) grupos.add(sesion.get("numero_de_clase").asLong());
        }
        return grupos;
    }

    //Se compara la firma en la propuesta del horario. No se puede guardar si hubo cambios
    //Bloquea las clases antes de hacer una confirmación
    //Actualiza los cupos y el promedio de calificación de la clase

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
