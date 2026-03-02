package br.centroweg.libera_ai.module.payment.infrastructure.persistence.repository;

import br.centroweg.libera_ai.module.payment.domain.model.Payment;
import br.centroweg.libera_ai.module.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentEntityRepositoryJpa extends JpaRepository<PaymentEntity, UUID> {


    Optional<PaymentEntity> findByAccessCode(int code);

    Optional<Payment> findByExternalId(String externalId);
}
