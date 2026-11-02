package com.health.project.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.health.project.entitys.Horario;


public interface HorarioRepository extends JpaRepository<Horario, Long> {
}


