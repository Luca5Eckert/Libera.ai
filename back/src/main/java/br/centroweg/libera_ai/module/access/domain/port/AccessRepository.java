package br.centroweg.libera_ai.module.access.domain.port;

import br.centroweg.libera_ai.module.access.domain.model.Access;

import java.util.Optional;

public interface AccessRepository {
    void save(Access access);

    Optional<Access> findByCodeAndExitIsNull(Integer code);
}
