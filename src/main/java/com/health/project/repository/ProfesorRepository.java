package com.health.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.health.project.entitys.Profesor;


public interface ProfesorRepository extends JpaRepository<Profesor, Long> {


}
