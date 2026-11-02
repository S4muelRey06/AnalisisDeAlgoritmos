
package com.health.project.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.health.project.entitys.Materia;
import com.health.project.repository.MateriaRepository;
import java.util.List;


@Service
public class MateriaServiceImpl implements MateriaService {

    private static final Long ID_CANCELADA = 2L;
    private static final Long ID_CONFIRMADA = 1L;
    
    @Autowired
    private MateriaRepository repo;

    @Override
    public List<Materia> buscarMaterias() {
        return repo.findAll();
    }

    @Override
    public Materia buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }
}