package com.health.project.service;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.health.project.entitys.ClaseHorario;

public interface ClaseHorarioService {

    List<ClaseHorario> buscarTodos();

    ClaseHorario buscarPorId(Long id);

    
}
