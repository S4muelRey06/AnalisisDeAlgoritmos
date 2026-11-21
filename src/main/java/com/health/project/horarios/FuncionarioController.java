package com.health.project.horarios;

import java.io.*;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/funcionario/clases")
public class FuncionarioController {
    private final ImportadorClase importador;
    public FuncionarioController(ImportadorClase importador) { this.importador=importador; }

    @GetMapping
    String formulario() { return "importar-clase"; }

    @PostMapping
    String cargar(@RequestParam MultipartFile archivo, Model model, RedirectAttributes flash) {
        try {
            long id=importador.importar(archivo);
            flash.addFlashAttribute("mensaje","Clase importada. Número de clase: "+id);
            return "redirect:/funcionario/clases";
        } catch (IOException | UncheckedIOException | IllegalArgumentException | DateTimeParseException ex) {
            model.addAttribute("error","No se importó la clase. "+ex.getMessage());
            return "importar-clase";
        }
    }
}
