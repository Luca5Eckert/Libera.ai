package br.centroweg.libera_ai.module.access.application.controller;

import br.centroweg.libera_ai.module.access.application.dto.AccessExitRequest;
import br.centroweg.libera_ai.module.access.application.dto.AccessExitResponse;
import br.centroweg.libera_ai.module.access.application.mapper.AccessMapper;
import br.centroweg.libera_ai.module.access.application.use_case.AccessExitUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/access")
public class AccessController {

    private final AccessMapper mapper;

    private final AccessExitUseCase accessExitUseCase;

    public AccessController(AccessMapper mapper, AccessExitUseCase accessExitUseCase) {
        this.mapper = mapper;
        this.accessExitUseCase = accessExitUseCase;
    }

    @PutMapping("/exit")
    public ResponseEntity<AccessExitResponse> exit(
            @RequestBody @Valid AccessExitRequest request
    ) {
        var access = accessExitUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toResponse(access));
    }

}
