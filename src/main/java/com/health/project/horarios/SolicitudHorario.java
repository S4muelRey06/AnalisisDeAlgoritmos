package com.health.project.horarios;

import java.util.LinkedHashMap;
import java.util.List;

/*
Esta es la estructura de los JSON de entradas, de todas las condiciones de entrada
*/
public record SolicitudHorario(LinkedHashMap<String,String> materias, List<String> profesores, Detalles detalles) {
    public record Detalles(List<String> franjas_horarias, Integer cercania, String volumen_de_compañeros, double promedio) {}
}
