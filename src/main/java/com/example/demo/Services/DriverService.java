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
    public String saveDriverPhoto(String driverId, MultipartFile photo) throws IOException {
        Driver d = driverRepo.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver nahi mila: " + driverId));

        // Folder banao
        File dir = new File("uploads/driver-photos/");
        if (!dir.exists()) dir.mkdirs();

        // Unique filename — driverId + UUID + extension
        String origName = photo.getOriginalFilename();
        String ext = (origName != null && origName.contains("."))
                ? origName.substring(origName.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        // Sirf allowed extensions
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            ext = ".jpg";
        }
        String fileName = driverId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path filePath = Paths.get("uploads/driver-photos/" + fileName);

        // Purani photo delete karo (agar thi)
        if (d.getPhotoPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(d.getPhotoPath()));
            } catch (Exception ignored) {}
        }

        // Naya file save karo
        Files.write(filePath, photo.getBytes());

        // DB mein path save karo
        d.setPhotoPath(filePath.toString());
        driverRepo.save(d);

        return "/api/drivers/" + driverId + "/photo";
    }

    // ── Get Photo Path ────────────────────────────────
    // Controller ko absolute path deta hai file serve karne ke liye
    public Path getDriverPhotoPath(String driverId) {
        Driver d = driverRepo.findByDriverId(driverId).orElse(null);
        if (d == null || d.getPhotoPath() == null) return null;
        return Paths.get(d.getPhotoPath()).toAbsolutePath();
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