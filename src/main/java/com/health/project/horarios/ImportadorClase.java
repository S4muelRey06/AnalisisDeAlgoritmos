package com.health.project.horarios;

import com.health.project.entitys.*;
import com.health.project.repository.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportadorClase {
    static final List<String> COLUMNAS = List.of("materia","profesor","edificio","salon","cupos","ocupados","suma_promedios","dia","inicio","fin");
    private final MateriaRepository materias;
    private final ProfesorRepository profesores;
    private final LugarRepository lugares;
    private final HorarioRepository horarios;
    private final ClaseRepository clases;
    private final ClaseHorarioRepository enlaces;

    public ImportadorClase(MateriaRepository materias, ProfesorRepository profesores, LugarRepository lugares,
            HorarioRepository horarios, ClaseRepository clases, ClaseHorarioRepository enlaces) {
        this.materias=materias; this.profesores=profesores; this.lugares=lugares;
        this.horarios=horarios; this.clases=clases; this.enlaces=enlaces;
    }

    @Transactional
    public long importar(MultipartFile archivo) throws IOException {
        if (archivo.isEmpty() || archivo.getSize()>100_000) throw new IllegalArgumentException("Elige un CSV de hasta 100 KB.");
        String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8).replaceFirst("^\uFEFF","");
        var formato = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get();
        try (var csv = CSVParser.parse(contenido,formato)) {
            if (!csv.getHeaderNames().equals(COLUMNAS)) throw new IllegalArgumentException("Usa los encabezados de la plantilla.");
            var filas = csv.getRecords();
            if (filas.isEmpty()) throw new IllegalArgumentException("Falta una sesión de clase.");
            var primera = filas.getFirst();
            List<Horario> sesiones = new ArrayList<>();
            for (var fila : filas) {
                if (!fila.isConsistent() || !primera.isConsistent()) throw new IllegalArgumentException("Número de columnas incorrecto.");
                for (String columna : COLUMNAS.subList(0,7))
                    if (fila.get(columna).isBlank() || !fila.get(columna).equals(primera.get(columna)))
                        throw new IllegalArgumentException("Un archivo corresponde a un grupo. Revisa "+columna+".");
                var h = new Horario(fila.get("dia"),LocalTime.parse(fila.get("inicio")),LocalTime.parse(fila.get("fin")));
                if (!List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado").contains(h.getDia())
                    || !h.isValidHoraInicio() || !h.isValidHoraFin() || !h.getHoraInicio().isBefore(h.getHoraFin())
                    || h.getHoraInicio().getMinute()!=0 || h.getHoraInicio().getSecond()!=0 || h.getHoraInicio().getNano()!=0 || h.getHoraFin().getMinute()!=0
                    || sesiones.stream().anyMatch(s -> s.getDia().equals(h.getDia()) && s.getHoraInicio().isBefore(h.getHoraFin()) && h.getHoraInicio().isBefore(s.getHoraFin())))
                    throw new IllegalArgumentException("Sesión inválida o repetida en la fila "+(fila.getRecordNumber()+1));
                sesiones.add(h);
            }
            int capacidad=Integer.parseInt(primera.get("cupos")), ocupados=Integer.parseInt(primera.get("ocupados"));
            double suma=Double.parseDouble(primera.get("suma_promedios"));
            long edificio=Long.parseLong(primera.get("edificio"));
            String salon=primera.get("salon"), nombre=primera.get("materia"), docente=primera.get("profesor");
            if (capacidad<1 || ocupados<0 || ocupados>capacidad || !Double.isFinite(suma) || suma<0 || suma>5.0*ocupados
                || edificio<1 || !salon.matches("[0-9][^\\s]{2}") || nombre.length()>100 || docente.length()>100)
                throw new IllegalArgumentException("Revisa los cupos, el promedio acumulado, el edificio, el salón y los nombres.");
            var materia=materias.findAll().stream().filter(m -> m.getNombre().equalsIgnoreCase(nombre)).findFirst()
                .orElseGet(() -> materias.save(new Materia(nombre)));
            var profesor=profesores.findAll().stream().filter(p -> p.getNombre().equalsIgnoreCase(docente)).findFirst()
                .orElseGet(() -> profesores.save(new Profesor(docente)));
            var lugar=lugares.findAll().stream().filter(l -> l.getNumeroEdificio()==edificio && l.getNumeroSalon().equals(salon)).findFirst()
                .orElseGet(() -> lugares.save(new Lugar(edificio,salon)));
            var clase=new Clase(profesor,lugar,materia,capacidad);
            clase.setCuposOcupados(ocupados);
            clase.setSumaPromedios(suma);
            clases.save(clase);
            for (var h : sesiones) enlaces.save(new ClaseHorario(clase,horarios.save(h)));
            return clase.getId();
        }
    }
}
