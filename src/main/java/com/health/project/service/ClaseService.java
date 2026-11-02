package com.health.project.service;

import java.util.List;

import com.health.project.entitys.Clase;

public interface ClaseService {

    List<Clase> buscarTodos();

    Clase buscarPorId(Long id);
}