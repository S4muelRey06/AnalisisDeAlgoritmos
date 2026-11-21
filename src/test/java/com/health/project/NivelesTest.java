package com.health.project;

import com.health.project.horarios.GeneradorHorario;
import com.health.project.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class NivelesTest {
    @Autowired ClaseRepository clases;
    @Autowired MateriaRepository materias;
    @Autowired ProfesorRepository profesores;
    static final String T="Taller de Prototipos", L="Laboratorio de Prototipos", S="Seminario de Diseño";
    GeneradorHorario.Resultado generar(List<String> nombres, String franja, String volumen, String profesor, int cerca, double promedio) {
        var catalogo=materias.findAll();
        var orden=nombres.stream().map(n -> catalogo.stream().filter(m -> n.equals(m.getNombre())).findFirst().orElseThrow()).toList();
        var docentes=profesores.findAll().stream().filter(p -> p.getNombre().equals(profesor)).map(p -> p.getId()).toList();
        return new GeneradorHorario().generar(orden,clases.findAll(),new GeneradorHorario.Preferencias(
            franja.isEmpty()?List.of():List.of(franja),volumen,docentes,cerca,promedio));
    }
    String docente(GeneradorHorario.Resultado r) {
        return r.horario().values().stream().flatMap(List::stream).filter(s -> s.materia().equals(S)).findFirst().orElseThrow().profesor().nombre();
    }
    @Test void sesionesPrioridadYCupos() {
        var r=generar(List.of(T,L,S),"","libre","",0,3);
        assertEquals(1,r.horario().get("Miércoles").size());
        assertEquals(2,r.horario().get("Lunes").size());
        assertEquals(L,r.no_asignadas().getFirst().materia());
        assertTrue(r.no_asignadas().getFirst().motivo().contains("sin cupos"));
        assertTrue(r.no_asignadas().getFirst().motivo().contains("cruce"));
        assertEquals(T,generar(List.of(L,T),"","libre","",0,3).no_asignadas().getFirst().materia());
    }
    @Test void franja() { assertEquals(1,generar(List.of("Proyecto Integrador"),"mañana","libre","",0,3).fuera_de_franja().size()); }
    @Test void volumen() {
        assertEquals("Leo Duarte",docente(generar(List.of(S),"mañana","bajo","Ana Solano",0,3)));
        assertEquals("Ana Solano",docente(generar(List.of(S),"mañana","alto","",0,3)));
        for(String v:List.of("alto","bajo")) assertTrue(generar(List.of(T),"",v,"",0,3).avisos().isEmpty());
    }
    @Test void profesor() { assertEquals("Ana Solano",docente(generar(List.of(S),"mañana","libre","Ana Solano",0,3))); }
    @Test void cercania() {
        assertEquals("Ana Solano",docente(generar(List.of(T,S),"","libre","",1,3)));
        assertEquals("Inés Sierra",docente(generar(List.of(T,S),"","libre","",2,3)));
    }
    @Test void promedio() {
        assertEquals("Leo Duarte",docente(generar(List.of(S),"mañana","libre","",0,3)));
        assertEquals("Ana Solano",docente(generar(List.of(S),"mañana","libre","",0,4.3)));
        var grupo=clases.findAll().stream().filter(c -> c.getMateria().getNombre().equals(S) && c.getProfesor().getNombre().equals("Leo Duarte")).findFirst().orElseThrow();
        grupo.setCuposOcupados(0);
        grupo.setSumaPromedios(0.0);
        assertEquals("Leo Duarte",docente(generar(List.of(S),"mañana","libre","",0,4.3)));
    }
    @Autowired com.health.project.horarios.ImportadorClase importador;

    @Test
    void aceptaSalonAlfanumericoConPiso() throws Exception {
        String csv = "materia,profesor,edificio,salon,cupos,ocupados,suma_promedios,dia,inicio,fin\n";
        csv += "Prueba Salón,Docente Prueba,2,3A2,20,0,0,Lunes,08:00,10:00\n";
        csv = csv.replace("\n", "\n");
        var archivo = new org.springframework.mock.web.MockMultipartFile("archivo","clase.csv","text/csv",
            csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long id = importador.importar(archivo);
        assertEquals("3A2",clases.findById(id).orElseThrow().getLugar().getNumeroSalon());
    }

    @Test
    void promedioConVariosGruposYVaciosIntercalados() {
        var plantilla=clases.findAll().stream().filter(c -> c.getMateria().getNombre().equals(S)).findFirst().orElseThrow();
        var candidatos=new ArrayList<com.health.project.entitys.Clase>();
        // Orden inicial: promedio bajo, vacío, alto, vacío, bajo, alto.
        double[] promedios={3.0,0,4.6,0,3.5,4.8};
        for(int i=0;i<promedios.length;i++) {
            var c=new com.health.project.entitys.Clase(plantilla.getProfesor(),plantilla.getLugar(),plantilla.getMateria(),20);
            c.setId(100L+i);
            c.setCuposOcupados(promedios[i]==0?0:5);
            c.setSumaPromedios(promedios[i]*5);
            c.setClaseHorarios(plantilla.getClaseHorarios());
            candidatos.add(c);
        }
        var p=new GeneradorHorario.Preferencias(List.of(),"libre",List.of(),0,4.3);
        var r=new GeneradorHorario().generar(List.of(plantilla.getMateria()),candidatos,p);
        assertEquals(102L,r.horario().values().stream().flatMap(List::stream).findFirst().orElseThrow().numero_de_clase());
        // Un vacío en la primera posición debe conservarla, aunque haya grupos afines detrás.
        candidatos.getFirst().setCuposOcupados(0);
        candidatos.getFirst().setSumaPromedios(0.0);
        r=new GeneradorHorario().generar(List.of(plantilla.getMateria()),candidatos,p);
        assertEquals(100L,r.horario().values().stream().flatMap(List::stream).findFirst().orElseThrow().numero_de_clase());
    }

    @Test
    void rechazaInicioConSegundosOFracciones() {
        for (String inicio : List.of("08:00:30","08:00:00.001")) {
            String csv = "materia,profesor,edificio,salon,cupos,ocupados,suma_promedios,dia,inicio,fin\n"
                + "Prueba Horas,Docente Horas,2,302,20,0,0,Lunes,"+inicio+",10:00\n";
            var archivo = new org.springframework.mock.web.MockMultipartFile("archivo","clase.csv","text/csv",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long cantidad = clases.count();
            assertThrows(IllegalArgumentException.class, () -> importador.importar(archivo));
            assertEquals(cantidad, clases.count());
        }
    }
}
