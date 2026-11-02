package com.health.project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.health.project.entitys.Estudiante;

public interface EstudianteService {
    public List<Estudiante> searchAll();
    public Estudiante searchById(Long id);
    List<Estudiante> buscarPorIds(List<Long> ids); 

}
