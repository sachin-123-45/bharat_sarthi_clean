package com.example.demo.bookingentity;



import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
public class Driver {

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDriverId() {
		return driverId;
	}

	public void setDriverId(String driverId) {
		this.driverId = driverId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAadhaarNumber() {
		return aadhaarNumber;
	}

	public void setAadhaarNumber(String aadhaarNumber) {
		this.aadhaarNumber = aadhaarNumber;
	}

	public String getLicenceNumber() {
		return licenceNumber;
	}

	public void setLicenceNumber(String licenceNumber) {
		this.licenceNumber = licenceNumber;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public LocalDateTime getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(LocalDateTime lastSeen) {
		this.lastSeen = lastSeen;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String driverId;          // e.g. DRV-001

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 20)
    private String aadhaarNumber;     // masked: XXXX-XXXX-4521

    @Column(nullable = false, length = 30)
    private String licenceNumber;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;          // plain text for simplicity (local use)

    @Column(nullable = false)
    private boolean online = false;

    @Column
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(length = 500)
    private String fcmToken;
    

    @Column(length = 255)
    private String aadhaarImagePath;

    @Column(length = 255)
    private String licenceImagePath;

    @Column(length = 255)
    private String photoPath;

	public String getAadhaarImagePath() {
		return aadhaarImagePath;
	}

	public void setAadhaarImagePath(String aadhaarImagePath) {
		this.aadhaarImagePath = aadhaarImagePath;
	}

	public String getLicenceImagePath() {
		return licenceImagePath;
	}

	public void setLicenceImagePath(String licenceImagePath) {
		this.licenceImagePath = licenceImagePath;
	}

	public String getPhotoPath() {
		return photoPath;
	}

	public void setPhotoPath(String photoPath) {
		this.photoPath = photoPath;
	}
	
	

	
	
	public String getFcmToken() {
	    return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
	    this.fcmToken = fcmToken;
	}
	
    
}