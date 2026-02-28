package br.centroweg.libera_ai.infrastructure.persistence.entity;

import br.centroweg.libera_ai.module.access.domain.model.Access;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int code;

    @Generated(event = EventType.INSERT)
    @Column(name = "entry_time",
            insertable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime entry;

    @Column(name = "exit_time", nullable = true)
    private LocalDateTime exit;

    public static AccessEntity of(Access access) {
        return new AccessEntity(
                access.getId(),
                access.getCode(),
                access.getEntry(),
                access.getExit()
        );
    }

    public Access toDomain() {
        return new Access(
                this.id,
                this.code,
                this.entry,
                this.exit
        );
    }

}