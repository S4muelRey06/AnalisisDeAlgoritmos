package com.health.project.service;

import java.util.List;

import com.health.project.entitys.Lugar;

public interface LugarService {

    List<Lugar> buscarTodos();

    Lugar buscarPorId(Long id);

    void guardar(Lugar Lugar);

}