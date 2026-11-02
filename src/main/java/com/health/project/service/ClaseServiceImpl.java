
package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.Clase;
import com.health.project.repository.ClaseRepository;
@Service
public class ClaseServiceImpl implements ClaseService {
    @Autowired
    private ClaseRepository repo;

    @Override
    public List<Clase> buscarTodos() {
        return repo.findAll();
    }

    @Override
    public Clase buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }



}


