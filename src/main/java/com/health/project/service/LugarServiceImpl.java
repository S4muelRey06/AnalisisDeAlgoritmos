package com.health.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.health.project.entitys.Lugar;
import com.health.project.repository.LugarRepository;

@Service
public class LugarServiceImpl implements LugarService {
    @Autowired
    private LugarRepository repo;

    @Override
    public List<Lugar> buscarTodos() {
        return repo.findAll();
    }

    @Override
    public Lugar buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void guardar(Lugar Lugar) {
        repo.save(Lugar);
    }


}