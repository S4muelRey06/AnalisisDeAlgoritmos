
package com.health.project.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.health.project.entitys.Materia;


@Service
public interface MateriaService {

    List<Materia> buscarMaterias();

    Materia buscarPorId(Long id);



}


