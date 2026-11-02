package com.health.project.entitys;
import java.sql.Time;
import java.time.LocalTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Pattern(regexp = "^(Lunes|Martes|Miércoles|Jueves|Viernes|Sábado)$")
    @Column(nullable = false)
    private String dia; 
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio; //

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin; //

    @AssertTrue(message = "La hora de inicio debe estar entre las 7:00 y las 19:00")
    public boolean isValidHoraInicio() {
        if (horaInicio == null) return true;
        LocalTime min = LocalTime.of(7, 0);
        LocalTime max = LocalTime.of(19, 0);
        return !horaInicio.isBefore(min) && !horaInicio.isAfter(max);
    }
    @AssertTrue(message = "La hora de fin debe estar entre las 8:00 y las 22:00")
    public boolean isValidHoraFin() {
        if (horaFin == null) return true;
        LocalTime min = LocalTime.of(8, 0);
        LocalTime max = LocalTime.of(22, 0);
        return !horaFin.isBefore(min) && !horaFin.isAfter(max);
    }

    public Horario(String dia, LocalTime horaInicio, LocalTime horaFin) {
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }
}
