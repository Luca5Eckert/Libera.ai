package br.centroweg.libera_ai.module.access.application.mapper;

import br.centroweg.libera_ai.module.access.application.dto.AccessExitResponse;
import br.centroweg.libera_ai.module.access.domain.model.Access;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class AccessMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AccessExitResponse toResponse(Access access) {
        if (access == null) return null;

        return new AccessExitResponse(
                access.getId(),
                access.getEntry() != null ? access.getEntry().format(FORMATTER) : null,
                access.getExit() != null ? access.getExit().format(FORMATTER) : null
        );
    }
}