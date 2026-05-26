package com.example.demo.bookingentity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 200)
    private String pickupLocation;

    @Column(nullable = false, length = 200)
    private String dropLocation;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 15)
    private String customerMobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column
    private LocalDateTime acceptedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ─── PAYMENT FIELDS ───────────────────────────────

    // PENDING → SCREENSHOT_UPLOADED → VERIFIED → REJECTED
    @Column(name = "payment_status", length = 30)
    private String paymentStatus = "PENDING";

    // Total fare (e.g. 2000.0)
    @Column(name = "total_amount")
    private Double totalAmount;

    // 15% advance — customer ne online admin ko diya (e.g. 300.0)
    @Column(name = "advance_amount")
    private Double advanceAmount;

    // 85% remaining — driver ko trip pe CASH milega (e.g. 1700.0)
    @Column(name = "remaining_cash_amount")
    private Double remainingCashAmount;

    // 15% commission — sirf DB mein, frontend ko KABHI nahi bhejenge
    @Column(name = "commission_amount")
    private Double commissionAmount;

    // Screenshot file path — jab customer upload kare
    @Column(name = "payment_screenshot", length = 500)
    private String paymentScreenshot;

    // Admin rejection reason (optional)
    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    // ─── ENUM ─────────────────────────────────────────

    public enum BookingStatus {
        PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    }
    
    
    private String customerFcmToken;

    // ─── GETTERS & SETTERS ────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String p) { this.pickupLocation = p; }

    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String d) { this.dropLocation = d; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate d) { this.startDate = d; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate d) { this.endDate = d; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String m) { this.customerMobile = m; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus s) { this.status = s; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver d) { this.driver = d; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime t) { this.acceptedAt = t; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String s) { this.paymentStatus = s; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double v) { this.totalAmount = v; }

    public Double getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(Double v) { this.advanceAmount = v; }

    public Double getRemainingCashAmount() { return remainingCashAmount; }
    public void setRemainingCashAmount(Double v) { this.remainingCashAmount = v; }

    public Double getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(Double v) { this.commissionAmount = v; }

    public String getPaymentScreenshot() { return paymentScreenshot; }
    public void setPaymentScreenshot(String s) { this.paymentScreenshot = s; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String s) { this.rejectionReason = s; }
    
    
    
    
    public String getCustomerFcmToken() {
        return customerFcmToken;
    }

    public void setCustomerFcmToken(String customerFcmToken) {
        this.customerFcmToken = customerFcmToken;
    }
}