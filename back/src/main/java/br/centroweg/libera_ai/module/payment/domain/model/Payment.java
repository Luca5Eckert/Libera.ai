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
    private String lastProcessedMpPaymentId;
    private String paymentStatus;

    private Payment(String id, Access access, double amount) {
        this.id = id;
        this.access = access;
        this.amount = amount;
        this.paid = false;
        this.paymentStatus = "pending";
    }

    public Payment(String id, Access access, double amount, boolean paid, String externalId) {
        this.id = id;
        this.access = access;
        this.amount = amount;
        this.paid = paid;
        this.externalId = externalId;
        this.paymentStatus = paid ? "approved" : "pending";
    }

    public Payment(String id, Access access, double amount, boolean paid, String externalId, 
                   String lastProcessedMpPaymentId, String paymentStatus) {
        this.id = id;
        this.access = access;
        this.amount = amount;
        this.paid = paid;
        this.externalId = externalId;
        this.lastProcessedMpPaymentId = lastProcessedMpPaymentId;
        this.paymentStatus = paymentStatus;
    }

    public static Payment of(Access access) {
        LocalDateTime exitTime = LocalDateTime.now();

        long minutes = Duration.between(access.getEntry(), exitTime).toMinutes();
        double calculatedAmount = calculateAmount(minutes);

        return new Payment(UUID.randomUUID().toString(), access, calculatedAmount);
    }

    private static double calculateAmount(long minutes) {
        if (minutes <= 0) return 2.0;

        double hours = Math.ceil(minutes / 60.0);
        return hours * 10.0;
    }

    /**
     * Process payment status update from Mercado Pago.
     * Returns true if this is a new notification, false if already processed (idempotency check).
     * 
     * Security notes:
     * - Once a payment is approved, we don't accept status changes from different MP payment IDs
     * - This prevents attacks where someone tries to change the status using a different payment
     * - We still allow the same MP payment ID to send updates (e.g., for refunds)
     */
    public boolean processStatusUpdate(String mercadoPagoPaymentId, String status) {
        // Idempotency check: skip if we already processed this MP payment ID with same status
        if (mercadoPagoPaymentId != null && mercadoPagoPaymentId.equals(this.lastProcessedMpPaymentId) 
            && status != null && status.equals(this.paymentStatus)) {
            return false;
        }
        
        // Security check: once approved, don't accept updates from a different MP payment ID
        // This prevents scenarios where someone tries to change the status using a different payment
        if (this.paid && this.lastProcessedMpPaymentId != null 
            && !this.lastProcessedMpPaymentId.equals(mercadoPagoPaymentId)) {
            // Already approved with a different payment ID - ignore this notification
            return false;
        }
        
        this.lastProcessedMpPaymentId = mercadoPagoPaymentId;
        this.paymentStatus = status;
        
        if ("approved".equalsIgnoreCase(status)) {
            this.paid = true;
        } else if ("rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            // Only set to false if not already approved (handle refund case separately if needed)
            if (!this.paid) {
                this.paid = false;
            }
        }
        // For "pending" and other statuses, keep current paid state
        
        return true;
    }

    /**
     * @deprecated Use processStatusUpdate for proper idempotency handling
     */
    @Deprecated
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

    public String getLastProcessedMpPaymentId() {
        return lastProcessedMpPaymentId;
    }

    public void setLastProcessedMpPaymentId(String lastProcessedMpPaymentId) {
        this.lastProcessedMpPaymentId = lastProcessedMpPaymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

}