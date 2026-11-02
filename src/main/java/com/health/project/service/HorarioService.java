package com.health.project.service;

import java.util.List;

import com.health.project.entitys.Horario;

public interface HorarioService {
    List<Horario> buscarTodos();

    Horario buscarPorId(Long id);
}
