package br.centroweg.libera_ai.module.payment.domain.model;

import br.centroweg.libera_ai.module.access.domain.model.Access;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {

    private String id;
    private Access access;
    private double amount;
    private boolean paid;
    private String externalId;

    private Payment(String id, Access access, double amount) {
        this.id = id;
        this.access = access;
        this.amount = amount;
        this.paid = false;
    }

    public Payment(String id, Access access, double amount, boolean paid, String externalId) {
        this.id = id;
        this.access = access;
        this.amount = amount;
        this.paid = paid;
        this.externalId = externalId;
    }

    public static Payment of(Access access) {
        LocalDateTime exitTime = LocalDateTime.now();

        long minutes = Duration.between(access.getEntry(), exitTime).toMinutes();
        double calculatedAmount = calculateAmount(minutes);

        return new Payment(UUID.randomUUID().toString(), access, calculatedAmount);
    }

    private static double calculateAmount(long minutes) {
        if (minutes <= 0) return 0.0;

        double hours = Math.ceil(minutes / 60.0);
        return hours * 10.0;
    }

    public void confirmPayment(String status) {
        if ("approved".equalsIgnoreCase(status)) {
            this.paid = true;
        }
    }

    public boolean isReadyForExit(){
        return paid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

}