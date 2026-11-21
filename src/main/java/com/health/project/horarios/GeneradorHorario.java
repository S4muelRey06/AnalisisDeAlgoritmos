package com.health.project.horarios;

import com.health.project.entitys.*;
import java.util.*;
import java.time.LocalTime;

public class GeneradorHorario {
    public record Preferencias(List<String> franjas, String volumen, List<Long> profesores, int cercania, double promedio) {}
    public record Docente(long id, String nombre) {}
    public record Franja(long id, String dia, String hora_inicio, String hora_fin) {}
    public record Aula(long id, String numero_edificio, String numero_salon) {}
    public record Sesion(String materia, long numero_de_clase, Docente profesor, Franja horario, Aula lugar) {}
    public record Pendiente(String materia, String motivo) {}
    public record Asignacion(String materia, long numero_de_clase) {}
    public record Resultado(Map<String,List<Sesion>> horario, List<Pendiente> no_asignadas,
                            List<Asignacion> fuera_de_franja, @com.fasterxml.jackson.annotation.JsonIgnore List<String> avisos) {}
    private static final List<String> DIAS = List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado");

    public Resultado generar(List<Materia> ordenadas, List<Clase> grupos, Preferencias p) {
        return generar(ordenadas, grupos, p, Set.of());
    }

    public Resultado generar(List<Materia> ordenadas, List<Clase> grupos, Preferencias p, Set<Long> yaInscrito) {
        Map<String,List<Sesion>> semana = new LinkedHashMap<>();
        DIAS.forEach(d -> semana.put(d,new ArrayList<>()));
        List<Pendiente> pendientes = new ArrayList<>();
        List<Asignacion> fuera = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        List<Clase> elegidas = new ArrayList<>();
        for (Materia materia : ordenadas) {
            List<Clase> disponibles = new ArrayList<>();
            Set<String> causas = new LinkedHashSet<>();
            for (Clase c : grupos) {
                if (!materia.getId().equals(c.getMateria().getId())) continue;
                if (!yaInscrito.contains(c.getId()) && c.getCuposOcupados() >= c.getCupos()) causas.add("sin cupos");
                else if (c.getClaseHorarios().isEmpty()) causas.add("sin horario publicado");
                else if (c.getClaseHorarios().stream().map(ClaseHorario::getHorario).anyMatch(h ->
                    semana.get(h.getDia()).stream().anyMatch(s -> h.getHoraInicio().isBefore(LocalTime.parse(s.horario().hora_fin())) && LocalTime.parse(s.horario().hora_inicio()).isBefore(h.getHoraFin()))))
                    causas.add("cruce de horarios");
                else disponibles.add(c);
            }
            if (disponibles.isEmpty()) {
                pendientes.add(new Pendiente(materia.getNombre(), causas.isEmpty() ? "sin grupos publicados" : String.join(", ", causas)));
                continue;
            }
            Comparator<Clase> orden = Comparator.comparing((Clase c) -> !enFranja(c,p))
                .thenComparing(c -> !volumenPreferido(c,p.volumen()))
                .thenComparing(c -> !p.profesores().contains(c.getProfesor().getId()))
                .thenComparing(c -> !cercana(c,elegidas,p.cercania()));
            disponibles.sort(orden);
            ordenarPorPromedio(disponibles, orden, p);
            Clase mejor = disponibles.getFirst();
            String nombre = materia.getNombre();
            if (!enFranja(mejor,p)) fuera.add(new Asignacion(nombre,mejor.getId()));
            if (!volumenPreferido(mejor,p.volumen())) avisos.add(nombre + ": volumen de compañeros distinto al solicitado.");
            if (!p.profesores().isEmpty() && !p.profesores().contains(mejor.getProfesor().getId()))
                avisos.add(nombre + ": el profesor asignado no está en tu lista preferida.");
            if (!cercana(mejor,elegidas,p.cercania())) avisos.add(nombre + ": no cumple la cercanía solicitada.");
            if (mejor.getCuposOcupados()>0 && !afin(mejor,p)) avisos.add(nombre + ": el promedio del grupo es inferior al indicado.");
            elegidas.add(mejor);
            for (ClaseHorario enlace : mejor.getClaseHorarios()) {
                Horario h = enlace.getHorario();
                var docente = mejor.getProfesor();
                var lugar = mejor.getLugar();
                semana.get(h.getDia()).add(new Sesion(nombre, mejor.getId(), new Docente(docente.getId(), docente.getNombre()),
                    new Franja(h.getId(), h.getDia(), h.getHoraInicio().toString(), h.getHoraFin().toString()),
                    new Aula(lugar.getId(), lugar.getNumeroEdificio().toString(), lugar.getNumeroSalon())));
            }
        }
        semana.values().forEach(s -> s.sort(Comparator.comparing(v -> v.horario().hora_inicio())));
        return new Resultado(semana,pendientes,fuera,avisos);
    }

    // Reordena solo los grupos inscritos dentro de cada empate de criterios anteriores.
    // Las posiciones ocupadas por grupos vacíos se conservan.
    void ordenarPorPromedio(List<Clase> grupos, Comparator<Clase> anteriores, Preferencias p) {
        int inicio = 0;
        while (inicio < grupos.size()) {
            int fin = inicio + 1;
            while (fin < grupos.size() && anteriores.compare(grupos.get(inicio), grupos.get(fin)) == 0) fin++;
            List<Clase> inscritos = new ArrayList<>();
            for (int i = inicio; i < fin; i++) {
                if (grupos.get(i).getCuposOcupados() > 0) inscritos.add(grupos.get(i));
            }
            inscritos.sort(Comparator.comparing(c -> !afin(c, p)));
            int siguiente = 0;
            for (int i = inicio; i < fin; i++) {
                if (grupos.get(i).getCuposOcupados() > 0) grupos.set(i, inscritos.get(siguiente++));
            }
            inicio = fin;
        }
    }

    private boolean afin(Clase c, Preferencias p) {
        return c.getCuposOcupados()>0 && c.getSumaPromedios()/c.getCuposOcupados() >= p.promedio();
    }

    private boolean volumenPreferido(Clase c, String volumen) {
        long doble = 2L*c.getCuposOcupados();
        return switch(volumen) {
            case "alto" -> doble >= c.getCupos();
            case "bajo" -> doble <= c.getCupos();
            default -> true;
        };
    }

    private boolean cercana(Clase c, List<Clase> elegidas, int nivel) {
        return nivel == 0 || elegidas.stream().allMatch(e ->
            Math.abs(c.getLugar().getNumeroEdificio()-e.getLugar().getNumeroEdificio()) <= (nivel == 1 ? 2 : 0));
    }

    private boolean enFranja(Clase c, Preferencias p) {
        return p.franjas().isEmpty() || c.getClaseHorarios().stream().allMatch(enlace -> {
            int hora = enlace.getHorario().getHoraInicio().getHour();
            return p.franjas().contains(hora < 12 ? "mañana" : hora < 18 ? "tarde" : "noche");
        });
    }
}
