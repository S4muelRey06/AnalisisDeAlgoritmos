package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.Profesor;
import com.health.project.repository.ProfesorRepository;

@Service 
public class ProfesorServiceImpl implements ProfesorService {

    @Autowired 
    private ProfesorRepository Profesorrepo;

    @Override
    public List<Profesor> searchAll() {
        return Profesorrepo.findAll();
    }

    @Override
    public Profesor searchById(Long id) {
        return Profesorrepo.findById(id).orElseThrow();
    }
    
}
