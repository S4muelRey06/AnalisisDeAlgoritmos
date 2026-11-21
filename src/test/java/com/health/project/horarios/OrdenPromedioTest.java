package com.health.project.horarios;

import com.health.project.entitys.Clase;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrdenPromedioTest {
    @Test
    void ordenaInscritosConservandoVaciosYOrdenEnEmpates() {
        List<Clase> grupos = new ArrayList<>();
        double[] promedios = {3.0, 0, 4.6, 0, 3.5, 4.8};
        for (int i = 0; i < promedios.length; i++) {
            Clase grupo = new Clase();
            grupo.setId(20L-i);
            grupo.setCuposOcupados(promedios[i] == 0 ? 0 : 5);
            grupo.setSumaPromedios(promedios[i]*5);
            grupos.add(grupo);
        }
        var original = new ArrayList<>(grupos);
        var preferencias = new GeneradorHorario.Preferencias(List.of(),"libre",List.of(),0,4.3);
        new GeneradorHorario().ordenarPorPromedio(grupos, (a,b) -> 0, preferencias);
        assertEquals(List.of(original.get(2),original.get(1),original.get(5),
            original.get(3),original.get(0),original.get(4)), grupos);
    }
}
