package com.example.demo.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.bookingentity.Driver;
import com.example.demo.bookingrepository.DriverRepository;
import com.example.demo.dtos.DriverDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DriverService {

    @Autowired private DriverRepository driverRepo;

    // ── Register ─────────────────────────────────────
    @Transactional
    public DriverDto.LoginResponse register(DriverDto.RegisterRequest req) {
        if (driverRepo.findByUsernameAndActiveTrue(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }
        Driver d = new Driver();
        long count = driverRepo.count() + 1;
        d.setDriverId("DRV-" + String.format("%03d", count));
        d.setName(req.getName());
        d.setMobile(req.getMobile());
        d.setAddress(req.getAddress());
        d.setAadhaarNumber(req.getAadhaarNumber());
        d.setLicenceNumber(req.getLicenceNumber());
        d.setUsername(req.getUsername());
        d.setPassword(req.getPassword());
        d.setActive(true);
        d.setOnline(false);
        driverRepo.save(d);
        return toLoginResponse(d);
    }

    // ── Login ─────────────────────────────────────────
    public DriverDto.LoginResponse login(String username, String password, String fcmToken) {
        Driver d = driverRepo.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("Driver nahi mila: " + username));

        if (!d.getPassword().equals(password)) {
            throw new RuntimeException("Galat password!");
        }

        d.setLastSeen(LocalDateTime.now());
        if (fcmToken != null && !fcmToken.isBlank()) {
            d.setFcmToken(fcmToken);
        }
        driverRepo.save(d);

        System.out.println("========== DRIVER LOGIN ==========");
        System.out.println("Driver ID  : " + d.getDriverId());
        System.out.println("FCM Token  : " + d.getFcmToken());
        System.out.println("==================================");

        return toLoginResponse(d);
    }

    // ── Toggle Online Status ──────────────────────────
    @Transactional
    public DriverDto.LoginResponse setOnlineStatus(String driverId, boolean online) {
        Driver d = driverRepo.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        d.setOnline(online);
        d.setLastSeen(LocalDateTime.now());
        driverRepo.save(d);
        return toLoginResponse(d);
    }

    // ── Photo Upload ──────────────────────────────────
    // Photo disk pe save karo, path DB mein store karo
    
    @Transactional
    public String saveDriverPhoto(String driverId, MultipartFile photo) throws IOException, InterruptedException {
        Driver d = driverRepo.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver nahi mila: " + driverId));

        // Base64 banao
        String base64Image = java.util.Base64.getEncoder()
                .encodeToString(photo.getBytes());

        // Imgur pe upload karo
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.imgur.com/3/image"))
                .header("Authorization", "Client-ID YOUR_IMGUR_CLIENT_ID")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("image=" + base64Image))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        // URL nikalo
        org.json.JSONObject json = new org.json.JSONObject(response.body());
        String photoUrl = json.getJSONObject("data").getString("link");

        d.setPhotoPath(photoUrl);
        driverRepo.save(d);
        return photoUrl;
    }

    // ── Get Photo Path ────────────────────────────────
    // Controller ko absolute path deta hai file serve karne ke liye
 // ✅ Yeh karo
    public Path getDriverPhotoPath(String driverId) {
        Driver d = driverRepo.findByDriverId(driverId).orElse(null);
        if (d == null || d.getPhotoPath() == null) return null;
        return Paths.get(d.getPhotoPath().replace("\\", "/")).toAbsolutePath();
    }

    // ── Helper: Driver → LoginResponse ───────────────
    private DriverDto.LoginResponse toLoginResponse(Driver d) {
        DriverDto.LoginResponse r = new DriverDto.LoginResponse();
        r.setId(String.valueOf(d.getId()));
        r.setDriverId(d.getDriverId());
        r.setName(d.getName());
        r.setMobile(d.getMobile());
        r.setAddress(d.getAddress());
        r.setAadhaar(d.getAadhaarNumber());
        r.setLicence(d.getLicenceNumber());
        r.setOnline(d.isOnline());
        // Photo URL bhi bhejo — frontend popup mein use hogi
        r.setPhotoUrl(d.getPhotoPath() != null
                ? "/api/drivers/" + d.getDriverId() + "/photo"
                : null);
        return r;
    }
    
    
    
    public void saveDriverToken(String driverId, String token) {

        Driver driver = driverRepo.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setFcmToken(token);

        driverRepo.save(driver);
    }
    
}