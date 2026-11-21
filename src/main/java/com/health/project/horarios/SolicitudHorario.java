package com.health.project.horarios;

import java.util.LinkedHashMap;
import java.util.List;

public record SolicitudHorario(LinkedHashMap<String,String> materias, List<String> profesores, Detalles detalles) {
    public record Detalles(List<String> franjas_horarias, Integer cercania, String volumen_de_compañeros, double promedio) {}
}
