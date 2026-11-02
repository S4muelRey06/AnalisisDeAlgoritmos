package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.Estudiante;
import com.health.project.repository.EstudianteRepository;
@Service 
public class EstudianteServiceImpl implements EstudianteService{
    @Autowired
    EstudianteRepository repo;

    @Override
    public List<Estudiante> searchAll() {
        return repo.findAll();
    }

    @Override
    public Estudiante searchById(Long id) {
        return repo.findById(id).orElseThrow();
    }
    public List<Estudiante> buscarPorIds(List<Long> ids) {
    return repo.findAllById(ids);
}
 
}
