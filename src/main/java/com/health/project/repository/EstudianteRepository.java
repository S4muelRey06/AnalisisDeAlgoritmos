package com.health.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.health.project.entitys.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    boolean existsByCorreoIgnoreCase(String correo);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Estudiante e where e.id = :id")
    java.util.Optional<Estudiante> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
