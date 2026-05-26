package com.example.demo.dtos;

import lombok.Data;
import java.time.LocalDate;

public class BookingDto {

    @Data
    public static class CreateRequest {
        private String pickupLocation;
        private String dropLocation;
        private LocalDate startDate;
        private LocalDate endDate;
        private String customerMobile;
        private String customerFcmToken;

        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
        public String getDropLocation() { return dropLocation; }
        public void setDropLocation(String dropLocation) { this.dropLocation = dropLocation; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getCustomerMobile() { return customerMobile; }
        public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }
        
        public String getCustomerFcmToken() {
            return customerFcmToken;
        }

        public void setCustomerFcmToken(String customerFcmToken) {
            this.customerFcmToken = customerFcmToken;
        }
    }

    @Data
    public static class AcceptRequest {
        private String driverId;
        public String getDriverId() { return driverId; }
        public void setDriverId(String driverId) { this.driverId = driverId; }
    }

    @Data
    public static class SetFareRequest {
        private Double totalAmount;
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    }

    @Data
    public static class Response {
        private String id;
        private String pickupLocation;
        private String dropLocation;
        private LocalDate startDate;
        private LocalDate endDate;
        private String customerMobile;
        private String status;
        private DriverInfo driver;
        private String paymentStatus;
        private Double totalAmount;
        private Double advanceAmount;
        private Double remainingCashAmount;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String p) { this.pickupLocation = p; }
        public String getDropLocation() { return dropLocation; }
        public void setDropLocation(String d) { this.dropLocation = d; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate s) { this.startDate = s; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate e) { this.endDate = e; }
        public String getCustomerMobile() { return customerMobile; }
        public void setCustomerMobile(String m) { this.customerMobile = m; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public DriverInfo getDriver() { return driver; }
        public void setDriver(DriverInfo d) { this.driver = d; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String p) { this.paymentStatus = p; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double t) { this.totalAmount = t; }
        public Double getAdvanceAmount() { return advanceAmount; }
        public void setAdvanceAmount(Double a) { this.advanceAmount = a; }
        public Double getRemainingCashAmount() { return remainingCashAmount; }
        public void setRemainingCashAmount(Double r) { this.remainingCashAmount = r; }

        @Data
        public static class DriverInfo {
            private String driverId;   // ✅ NAYA — photo URL ke liye zaroori
            private String name;
            private String mobile;
            private String address;
            private String aadhaar;
            private String licence;
            private String photoUrl;   // ✅ NAYA — driver ki real photo URL

            public String getDriverId() { return driverId; }
            public void setDriverId(String driverId) { this.driverId = driverId; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getMobile() { return mobile; }
            public void setMobile(String mobile) { this.mobile = mobile; }
            public String getAddress() { return address; }
            public void setAddress(String address) { this.address = address; }
            public String getAadhaar() { return aadhaar; }
            public void setAadhaar(String aadhaar) { this.aadhaar = aadhaar; }
            public String getLicence() { return licence; }
            public void setLicence(String licence) { this.licence = licence; }
            public String getPhotoUrl() { return photoUrl; }
            public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        }
    }
}