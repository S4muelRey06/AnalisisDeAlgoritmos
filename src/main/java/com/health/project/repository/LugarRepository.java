package com.health.project.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.health.project.entitys.Lugar;
import java.time.LocalDate;
import java.time.LocalTime;


public interface LugarRepository extends JpaRepository<Lugar, Long>{

}

