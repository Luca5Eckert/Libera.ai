package br.centroweg.libera_ai.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "access")
public record AccessEntity(
        int id,
        int code,
        LocalDateTime entry,
        LocalDateTime exit
) {
}
