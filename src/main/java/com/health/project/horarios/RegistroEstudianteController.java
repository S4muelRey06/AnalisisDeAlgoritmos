package com.health.project.horarios;

import com.health.project.entitys.Estudiante;
import com.health.project.repository.EstudianteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegistroEstudianteController {
    public record Registro(
        @NotBlank(message="Escribe tu nombre.") @Size(max=100, message="Usa hasta 100 caracteres.") String nombre,
        @NotBlank(message="Escribe tu correo.") @Email(message="Escribe un correo válido.")
        @Size(max=120, message="Usa hasta 120 caracteres.") String correo,
        @NotNull(message="Escribe tu promedio.") @DecimalMin(value="0", message="El promedio debe estar entre 0 y 5.")
        @DecimalMax(value="5", message="El promedio debe estar entre 0 y 5.") java.math.BigDecimal promedio) {
        public Registro {
            nombre = nombre == null ? "" : nombre.strip();
            correo = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        }
    }

    private final EstudianteRepository estudiantes;

    public RegistroEstudianteController(EstudianteRepository estudiantes) {
        this.estudiantes = estudiantes;
    }

    @GetMapping("/estudiantes/nuevo")
    String formulario(Model model) {
        model.addAttribute("registro",new Registro("","",null));
        return "registro-estudiante";
    }

    @PostMapping("/estudiantes/nuevo")
    String registrar(@Valid @ModelAttribute("registro") Registro registro, BindingResult errores) {
        if (errores.hasErrors()) return "registro-estudiante";
        if (estudiantes.existsByCorreoIgnoreCase(registro.correo())) {
            errores.rejectValue("correo","duplicado","Este correo ya está registrado. Elige tu nombre en la página de estudiantes.");
            return "registro-estudiante";
        }
        try {
            var estudiante = estudiantes.saveAndFlush(new Estudiante(registro.correo(),registro.nombre(),registro.promedio().doubleValue()));
            return "redirect:/estudiantes/"+estudiante.getId()+"/horario";
        } catch (DataIntegrityViolationException ex) {
            errores.rejectValue("correo","duplicado","No se pudo registrar este correo. Comprueba si ya existe.");
            return "registro-estudiante";
        }
    }
}
