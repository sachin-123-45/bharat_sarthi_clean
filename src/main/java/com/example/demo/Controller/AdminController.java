package com.example.demo.Controller;

import com.example.demo.Services.AdminService;
import com.example.demo.Services.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private BookingService bookingService;

    // ══ POST /api/admin/login ═════════════════════════════════════════
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        try {
            String username = req.get("username");
            String password = req.get("password");
            if (username == null || password == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username aur password dono zaroori hain"));
            }
            Map<String, Object> result = adminService.login(username, password);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Galat credentials: " + e.getMessage()));
        }
    }

    // ══ NEW: POST /api/admin/fcm-token ════════════════════════════════
    //
    // Admin app login ke baad apna FCM token yahan save karta hai.
    // Body: { "adminId": 1, "fcmToken": "device_fcm_token_string" }
    //
    // Admin app mein sirf ek baar call karo — login hone ke baad.
    // FCM token kabhi kabhi refresh hota hai (FirebaseMessaging.onTokenRefresh)
    // to tab bhi yahi endpoint call karna.
    //
    @PostMapping("/fcm-token")
    public ResponseEntity<?> saveFcmToken(@RequestBody Map<String, Object> req) {
        try {
            Long adminId = Long.valueOf(req.get("adminId").toString());
            String fcmToken = (String) req.get("fcmToken");

            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "fcmToken required hai"));
            }

            adminService.saveAdminFcmToken(adminId, fcmToken);

            return ResponseEntity.ok(Map.of(
                    "message", "FCM token save ho gaya",
                    "adminId", adminId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ══ GET /api/admin/dashboard ══════════════════════════════════════
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            return ResponseEntity.ok(adminService.getDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ GET /api/admin/bookings ═══════════════════════════════════════
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(adminService.getAllBookings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ GET /api/admin/bookings/pending-payments ══════════════════════
    @GetMapping("/bookings/pending-payments")
    public ResponseEntity<?> getPendingPayments() {
        try {
            return ResponseEntity.ok(adminService.getPendingPaymentBookings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ POST /api/admin/bookings/{id}/approve ═════════════════════════
    @PostMapping("/bookings/{id}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminService.approvePayment(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ POST /api/admin/bookings/{id}/reject ══════════════════════════
    @PostMapping("/bookings/{id}/reject")
    public ResponseEntity<?> rejectPayment(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = (body != null && body.get("reason") != null)
                    ? body.get("reason") : "Payment verify nahi hui";
            return ResponseEntity.ok(adminService.rejectPayment(id, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ GET /api/admin/drivers ════════════════════════════════════════
    @GetMapping("/drivers")
    public ResponseEntity<?> getAllDrivers() {
        try {
            return ResponseEntity.ok(adminService.getAllDrivers());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ POST /api/admin/drivers/{driverId}/block ══════════════════════
    @PostMapping("/drivers/{driverId}/block")
    public ResponseEntity<?> blockDriver(@PathVariable String driverId) {
        try {
            return ResponseEntity.ok(adminService.toggleDriverBlock(driverId, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ POST /api/admin/drivers/{driverId}/unblock ════════════════════
    @PostMapping("/drivers/{driverId}/unblock")
    public ResponseEntity<?> unblockDriver(@PathVariable String driverId) {
        try {
            return ResponseEntity.ok(adminService.toggleDriverBlock(driverId, false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══ GET /api/admin/screenshot ═════════════════════════════════════
    @GetMapping("/screenshot")
    public ResponseEntity<?> getScreenshot(@RequestParam String path) {
        try {
            Path filePath = Paths.get(path).normalize();
            if (!filePath.toString().startsWith("uploads")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = "image/jpeg";
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".png"))      contentType = "image/png";
            else if (fileName.endsWith(".gif")) contentType = "image/gif";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}