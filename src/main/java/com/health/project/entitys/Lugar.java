package com.health.project.entitys;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Setter 
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_edificio", nullable = false)
    private Long numeroEdificio; // Para calcular cercanía (n +- 2)

    @Column(name = "numero_salon", length = 3, nullable = false)
    private String numeroSalon;

    @OneToMany(mappedBy = "lugar")
    @JsonIgnore
    private List<Clase> clases;

    public Lugar(Long numeroEdificio, String numeroSalon) {
        this.numeroEdificio = numeroEdificio;
        this.numeroSalon = numeroSalon;
    }
}