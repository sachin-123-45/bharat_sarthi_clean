package com.example.demo.dtos;

 // apna package name likhna

public class PaymentRequest {
    private String paymentMethod; // "UPI" ya "CASH"
    private Double totalAmount;

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String m) { this.paymentMethod = m; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double a) { this.totalAmount = a; }
}