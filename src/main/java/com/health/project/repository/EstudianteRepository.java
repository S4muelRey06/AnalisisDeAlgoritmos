package com.health.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.health.project.entitys.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

}
