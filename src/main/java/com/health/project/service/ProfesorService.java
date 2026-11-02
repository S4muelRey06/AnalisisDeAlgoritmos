package com.health.project.service;

import java.util.List;

import com.health.project.entitys.Profesor;

public interface ProfesorService {
    public List<Profesor> searchAll();
    public Profesor searchById(Long id);

}
