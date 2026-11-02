package com.health.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.health.project.entitys.Clase;
import com.health.project.entitys.ClaseHorario;
import com.health.project.entitys.Estudiante;
import com.health.project.entitys.Horario;
import com.health.project.entitys.Lugar;
import com.health.project.entitys.Materia;
import com.health.project.entitys.Profesor;
import com.health.project.repository.ClaseHorarioRepository;
import com.health.project.repository.ClaseRepository;
import com.health.project.repository.EstudianteRepository;
import com.health.project.repository.HorarioRepository;
import com.health.project.repository.LugarRepository;
import com.health.project.repository.MateriaRepository;
import com.health.project.repository.ProfesorRepository;

import jakarta.transaction.Transactional;

import java.time.LocalTime;

@Component
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ProfesorRepository profesorRepo;

    @Autowired
    private MateriaRepository materiaRepo;

    @Autowired
    private HorarioRepository horarioRepo;

    @Autowired
    private LugarRepository lugarRepo;

    @Autowired
    private ClaseRepository claseRepo;

    @Autowired
    private ClaseHorarioRepository claseHorarioRepo;

    @Autowired
    private EstudianteRepository estudianteRepo;

    @Override
    public void run(String... args) throws Exception {

        // 1. Profesores (Basados en los ejemplos del documento)
        Profesor prof1 = profesorRepo.save(new Profesor("Alexander Castillo Martinez"));
        Profesor prof2 = profesorRepo.save(new Profesor("Gerardo Taborda Galvis"));
        Profesor prof3 = profesorRepo.save(new Profesor("Yesid Lara Gonzalez"));
        Profesor prof4 = profesorRepo.save(new Profesor("Laura Rodriguez Mora"));
        Profesor prof5 = profesorRepo.save(new Profesor("Carlos Alberto Gómez"));
        Profesor prof6 = profesorRepo.save(new Profesor("Ana María Rodríguez"));
        Profesor prof7 = profesorRepo.save(new Profesor("Jorge Enrique Martinez"));

        // 2. Materias (Basadas en los ejemplos del documento)
        Materia mat1 = materiaRepo.save(new Materia("Matemáticas Discretas"));
        Materia mat2 = materiaRepo.save(new Materia("Estructuras de Datos"));
        Materia mat3 = materiaRepo.save(new Materia("Física Mecánica"));
        Materia mat4 = materiaRepo.save(new Materia("Desarrollo Web"));
        Materia mat5 = materiaRepo.save(new Materia("Álgebra Lineal"));

        // 3. Lugares (Edificio y salón de 3 caracteres, ej: piso 3 salón 01 -> "301")
        Lugar lugar1 = lugarRepo.save(new Lugar(1L, "301"));
        Lugar lugar2 = lugarRepo.save(new Lugar(2L, "305"));
        Lugar lugar3 = lugarRepo.save(new Lugar(1L, "102"));
        Lugar lugar4 = lugarRepo.save(new Lugar(3L, "201"));

        // 4. Horarios (Franjas temporales de Lunes a Sábado, entre 7:00 y 22:00)
        Horario hor1 = horarioRepo.save(new Horario("Lunes", LocalTime.of(7, 0), LocalTime.of(9, 0)));
        Horario hor2 = horarioRepo.save(new Horario("Lunes", LocalTime.of(9, 0), LocalTime.of(11, 0)));
        Horario hor3 = horarioRepo.save(new Horario("Lunes", LocalTime.of(14, 0), LocalTime.of(16, 0)));
        Horario hor4 = horarioRepo.save(new Horario("Martes", LocalTime.of(11, 0), LocalTime.of(13, 0)));
        Horario hor5 = horarioRepo.save(new Horario("Martes", LocalTime.of(16, 0), LocalTime.of(18, 0)));
        Horario hor6 = horarioRepo.save(new Horario("Miércoles", LocalTime.of(7, 0), LocalTime.of(9, 0)));
        Horario hor7 = horarioRepo.save(new Horario("Viernes", LocalTime.of(7, 0), LocalTime.of(9, 0)));

        // 5. Clases (Tabla central que une materia, profesor, lugar, cupos)
        // Clase 101: Estructuras de Datos
        Clase clase1 = new Clase();
        clase1.setMateria(mat2);
        clase1.setProfesor(prof5);
        clase1.setLugar(lugar1);
        clase1.setCupos(40);
        clase1.setCuposOcupados(15);
        clase1.setSumaPromedios(63.0); // Promedio ponderado de inscritos
        clase1 = claseRepo.save(clase1);

        // Clase 204: Física Mecánica
        Clase clase2 = new Clase();
        clase2.setMateria(mat3);
        clase2.setProfesor(prof6);
        clase2.setLugar(lugar2);
        clase2.setCupos(35);
        clase2.setCuposOcupados(20);
        clase2.setSumaPromedios(86.0);
        clase2 = claseRepo.save(clase2);

        // Clase 102: Matemáticas Discretas
        Clase clase3 = new Clase();
        clase3.setMateria(mat1);
        clase3.setProfesor(prof7);
        clase3.setLugar(lugar3);
        clase3.setCupos(30);
        clase3.setCuposOcupados(10);
        clase3.setSumaPromedios(42.0);
        clase3 = claseRepo.save(clase3);

        // 6. Relación Clase-Horario (Tabla intermedia)
        claseHorarioRepo.save(new ClaseHorario(clase1, hor1)); // Estructuras de Datos el Lunes 7-9
        claseHorarioRepo.save(new ClaseHorario(clase2, hor2)); // Física Mecánica el Lunes 9-11
        claseHorarioRepo.save(new ClaseHorario(clase3, hor4)); // Matemáticas Discretas el Martes 11-13

        // 7. Estudiantes de prueba (Basado en la pantalla de selección de estudiantes del mockup)
        estudianteRepo.save(new Estudiante("samuel@gmail.com", "Samuel Rey", 4.2));
        estudianteRepo.save(new Estudiante("mateo.zamora@javeriana.edu.co", "Mateo Zamora", 4.7));
        estudianteRepo.save(new Estudiante("luisa.negrete@javeriana.edu.co", "Luisa Negrete", 4.5));
        estudianteRepo.save(new Estudiante("sara.caceres@javeriana.edu.co", "Sara Caceres", 4.4));
        estudianteRepo.save(new Estudiante("lucia.ortiz@javeriana.edu.co", "Lucia Ortiz", 3.8));

        System.out.println("¡Datos iniciales cargados con éxito en la base de datos!");
    }
}