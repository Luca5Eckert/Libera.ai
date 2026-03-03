package br.centroweg.libera_ai.module.payment.infrastructure.persistence.entity;

import br.centroweg.libera_ai.module.access.infrastructure.persistence.entity.AccessEntity;
import br.centroweg.libera_ai.module.payment.domain.model.Payment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(
            fetch = FetchType.EAGER,
            optional = false
    )
    @JoinColumn(name = "access_id")
    private AccessEntity access;

    private double amount;

    private boolean paid;

    private String externalId;

    private String lastProcessedMpPaymentId;

    private String paymentStatus;

    public PaymentEntity(AccessEntity access, double amount, boolean paid, String externalId) {
        this.access = access;
        this.amount = amount;
        this.paid = paid;
        this.externalId = externalId;
    }

    public static PaymentEntity of(Payment payment) {
        PaymentEntity entity = new PaymentEntity(
                AccessEntity.of(payment.getAccess()),
                payment.getAmount(),
                payment.isPaid(),
                payment.getExternalId()
        );
        entity.setLastProcessedMpPaymentId(payment.getLastProcessedMpPaymentId());
        entity.setPaymentStatus(payment.getPaymentStatus());
        return entity;
    }

    public Payment toDomain() {
        return new Payment(
                this.id.toString(),
                this.access.toDomain(),
                this.amount,
                this.paid,
                this.externalId,
                this.lastProcessedMpPaymentId,
                this.paymentStatus
        );
    }
}
