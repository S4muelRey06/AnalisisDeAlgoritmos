package com.health.project.entitys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clase_horarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
public class ClaseHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties({"profesor", "lugar", "materia"})
    private Clase clase;

    @ManyToOne(fetch = FetchType.LAZY)
    private Horario horario;

    public ClaseHorario(Clase clase, Horario horario) {
        this.clase = clase;
        this.horario = horario;
    }
}