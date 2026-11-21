package com.health.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.health.project.entitys.Clase;

public interface ClaseRepository extends JpaRepository<Clase, Long> {


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Clase e where e.id = :id")
    java.util.Optional<Clase> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
