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

@org.springframework.context.annotation.Profile("demo")
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

        if (materiaRepo.count() > 0) return;

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

        Clase alternativa = new Clase();
        alternativa.setMateria(mat1);
        alternativa.setProfesor(prof1);
        alternativa.setLugar(lugar2);
        alternativa.setCupos(25);
        alternativa = claseRepo.save(alternativa);
        claseHorarioRepo.save(new ClaseHorario(alternativa, hor1));
        claseHorarioRepo.save(new ClaseHorario(alternativa, hor6));

        // 7. Estudiantes de prueba (Basado en la pantalla de selección de estudiantes del mockup)
        estudianteRepo.save(new Estudiante("samuel@gmail.com", "Samuel Rey", 4.2));
        estudianteRepo.save(new Estudiante("mateo.zamora@javeriana.edu.co", "Mateo Zamora", 4.7));
        estudianteRepo.save(new Estudiante("luisa.negrete@javeriana.edu.co", "Luisa Negrete", 4.5));
        estudianteRepo.save(new Estudiante("sara.caceres@javeriana.edu.co", "Sara Caceres", 4.4));
        estudianteRepo.save(new Estudiante("lucia.ortiz@javeriana.edu.co", "Lucia Ortiz", 3.8));

        List<String> asignaturas = List.of("Bases de Datos", "Programación Orientada a Objetos",
            "Cálculo Diferencial", "Cálculo Integral", "Probabilidad", "Estadística Aplicada",
            "Sistemas Operativos", "Redes de Computadores", "Ingeniería de Software",
            "Arquitectura de Computadores", "Inteligencia Artificial", "Análisis de Algoritmos");
        List<String> docentes = List.of("Mónica Pardo", "Felipe Beltrán", "Adriana Nieto",
            "Tomás Galindo", "Diana Cárdenas", "Julián Rojas", "Claudia Bernal", "Andrés Lozano",
            "Paula Restrepo", "Ricardo Ávila", "Natalia Correa", "Daniel Quiroga");
        List<Profesor> equipo = docentes.stream().map(Profesor::new).map(profesorRepo::save).toList();
        List<String> dias = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado");
        int[] horas = {8, 14, 18};
        List<List<Horario>> semana = new ArrayList<>();
        for (String dia : dias) {
            List<Horario> turnos = new ArrayList<>();
            for (int hora : horas) {
                turnos.add(horarioRepo.save(new Horario(dia, LocalTime.of(hora, 0), LocalTime.of(hora + 2, 0))));
            }
            semana.add(turnos);
        }
        for (int i = 0; i < asignaturas.size(); i++) {
            Materia materia = materiaRepo.save(new Materia(asignaturas.get(i)));
            Lugar aula = lugarRepo.save(new Lugar(4L + i / 3, Integer.toString(201 + i % 3)));
            for (int turno = 0; turno < horas.length; turno++) {
                int capacidad = 24 + 6 * (i % 4);
                Clase grupo = new Clase(equipo.get((i + turno) % equipo.size()), aula, materia, capacidad);
                int inscritos = switch ((i + turno) % 5) {
                    case 0 -> capacidad;
                    case 1 -> capacidad / 3;
                    case 2 -> capacidad / 2;
                    case 3 -> capacidad - 3;
                    default -> 0;
                };
                grupo.setCuposOcupados(inscritos);
                grupo.setSumaPromedios(inscritos * (3.5 + (i + turno) % 10 / 10.0));
                claseRepo.save(grupo);
                int dia = (i + turno) % 3;
                claseHorarioRepo.save(new ClaseHorario(grupo, semana.get(dia).get(turno)));
                claseHorarioRepo.save(new ClaseHorario(grupo, semana.get(dia + 3).get(turno)));
            }
        }

        List<String> nombres = List.of("Valentina", "Sebastián", "Gabriela", "Nicolás",
            "Mariana", "Emilio", "Camila", "Simón", "Isabella", "Alejandro");
        List<String> apellidos = List.of("Ariza", "Montoya", "Vargas", "Pinzón", "Ospina", "Salazar");
        int consecutivo = 0;
        for (String apellido : apellidos) {
            for (String nombre : nombres) {
                consecutivo++;
                estudianteRepo.save(new Estudiante("estudiante" + consecutivo + "@example.org",
                    nombre + " " + apellido, (31 + consecutivo * 7 % 19) / 10.0));
            }
        }
        // Catálogo controlado para comprobar los niveles de la entrega.
        Materia taller = materiaRepo.save(new Materia("Taller de Prototipos"));
        Materia laboratorio = materiaRepo.save(new Materia("Laboratorio de Prototipos"));
        Materia seminario = materiaRepo.save(new Materia("Seminario de Diseño"));
        Materia proyecto = materiaRepo.save(new Materia("Proyecto Integrador"));
        Lugar base = lugarRepo.save(new Lugar(10L, "401"));
        Lugar cerca = lugarRepo.save(new Lugar(12L, "402"));
        Lugar lejos = lugarRepo.save(new Lugar(18L, "403"));
        Profesor ana = profesorRepo.save(new Profesor("Ana Solano"));
        Profesor leo = profesorRepo.save(new Profesor("Leo Duarte"));
        Profesor ines = profesorRepo.save(new Profesor("Inés Sierra"));
        grupo(taller, ana, base, 10, 4.0, hor1, hor6);
        grupo(laboratorio, leo, lejos, 10, 4.0, hor6);
        grupo(laboratorio, ines, cerca, 20, 4.5, hor4);
        grupo(seminario, leo, lejos, 6, 3.5, hor2);
        grupo(seminario, ana, cerca, 16, 4.6, hor2);
        grupo(seminario, ines, base, 10, 4.2, hor3);
        grupo(proyecto, ana, base, 20, 4.6, hor5);
        grupo(proyecto, leo, lejos, 8, 4.0, hor3);
        System.out.println("¡Datos iniciales cargados con éxito en la base de datos!");
    }

    private void grupo(Materia materia, Profesor profesor, Lugar lugar, int inscritos,
                       double promedio, Horario... sesiones) {
        Clase grupo = new Clase(profesor, lugar, materia, 20);
        grupo.setCuposOcupados(inscritos);
        grupo.setSumaPromedios(inscritos * promedio);
        claseRepo.save(grupo);
        for (Horario sesion : sesiones) {
            claseHorarioRepo.save(new ClaseHorario(grupo, sesion));
        }
    }
}