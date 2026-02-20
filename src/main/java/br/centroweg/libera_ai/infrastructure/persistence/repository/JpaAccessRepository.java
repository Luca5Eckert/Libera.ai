package br.centroweg.libera_ai.infrastructure.persistence.repository;

import br.centroweg.libera_ai.infrastructure.persistence.entity.AccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaAccessRepository extends JpaRepository<AccessEntity, Integer> {
    Optional<AccessEntity> findByCodeAndExitIsNull(Integer code);
}
