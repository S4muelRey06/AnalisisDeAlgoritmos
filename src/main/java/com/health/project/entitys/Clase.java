package com.health.project.entitys;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Código que el estudiante tiene en cuenta al inscribir[cite: 5]

    @ManyToOne
    private Profesor profesor;

    @ManyToOne
    private Lugar lugar;

    @ManyToOne
    private Materia materia;

    @Column(nullable = false)
    private Integer cupos;

    @Column(name = "cupos_ocupados", nullable = false)
    private Integer cuposOcupados = 0;

    @Column(name = "suma_promedios", nullable = false)
    private Double sumaPromedios = 0.0;

    @OneToMany(mappedBy = "clase")
    @JsonIgnore
    private List<ClaseHorario> claseHorarios;

    @AssertTrue(message = "Los cupos ocupados no pueden ser mayores a los cupos totales")
    public boolean isValidCuposOcupados() {
        if (cupos == null || cuposOcupados == null) return true;
        return cuposOcupados <= cupos;
    }

    public Clase(Profesor profesor, Lugar lugar, Materia materia, Integer cupos) {
        this.profesor = profesor;
        this.lugar = lugar;
        this.materia = materia;
        this.cupos = cupos;
        this.cuposOcupados = 0;
        this.sumaPromedios = 0.0;
    }
    
}
