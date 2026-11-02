package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.Horario;
import com.health.project.repository.HorarioRepository;
@Service 
public class HorarioServiceImpl implements HorarioService{

    @Autowired 
    HorarioRepository repo;

    @Override
    public List<Horario> buscarTodos() {
        return repo.findAll();
        
    }

    @Override
    public Horario buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }
    
}
