package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.ClaseHorario;
import com.health.project.repository.ClaseHorarioRepository;


@Service
public class ClaseHorarioServiceImpl implements ClaseHorarioService {
    @Autowired
    private ClaseHorarioRepository repo;

    @Override
    public List<ClaseHorario> buscarTodos() {
        return repo.findAll();
    }

    @Override
    public ClaseHorario buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }   
    
}