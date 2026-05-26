package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Services.BookingService;
import com.example.demo.dtos.BookingDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired private BookingService bookingService;

    // POST /api/bookings — Nai booking banao
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingDto.CreateRequest req) {
        try {
            BookingDto.Response r = bookingService.createBooking(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(r);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/bookings/{id} — Status check (customer polling)
    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bookingService.getBooking(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/bookings?status=PENDING — Driver ke liye pending list
    @GetMapping
    public ResponseEntity<List<BookingDto.Response>> getBookings(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(bookingService.getPendingBookings());
    }

    // POST /api/bookings/{id}/accept — Driver accept kare
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptBooking(
            @PathVariable String id,
            @RequestBody BookingDto.AcceptRequest req) {
        try {
            BookingDto.Response r = bookingService.acceptBooking(id, req.getDriverId());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/bookings/{id} — Customer cancel kare
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable String id) {
        try {
            bookingService.cancelBooking(id);
            return ResponseEntity.ok(Map.of("message", "Booking cancel ho gayi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/bookings/{id}/set-fare — Total fare set karo, advance calculate hoga
    // Body: { "totalAmount": 2000.0 }
    @PostMapping("/{id}/set-fare")
    public ResponseEntity<?> setFare(
            @PathVariable String id,
            @RequestBody BookingDto.SetFareRequest req) {
        try {
            BookingDto.Response r = bookingService.setTotalAndCalculateAdvance(id, req.getTotalAmount());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/bookings/{id}/upload-screenshot — Customer screenshot upload kare
    @PostMapping("/{id}/upload-screenshot")
    public ResponseEntity<?> uploadScreenshot(
            @PathVariable String id,
            @RequestParam("screenshot") MultipartFile screenshot) {
        try {
            if (screenshot.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Screenshot file empty hai"));
            }
            // BookingService ka uploadPaymentScreenshot method call karo
            // Woh khud file save karega aur DB update karega
            BookingDto.Response r = bookingService.uploadPaymentScreenshot(id, screenshot);
            return ResponseEntity.ok(Map.of(
                "message",       "Screenshot upload ho gayi! Admin verify karega.",
                "bookingId",     id,
                "paymentStatus", "SCREENSHOT_UPLOADED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
 // POST /api/bookings/{id}/complete — Driver ride complete kare
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRide(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String driverId = body.get("driverId");
            if (driverId == null || driverId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "driverId required hai"));
            }
            BookingDto.Response r = bookingService.completeRide(id, driverId);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/bookings/driver/{driverId}/active — Driver ki active bookings
    @GetMapping("/driver/{driverId}/active")
    public ResponseEntity<?> getDriverActiveBookings(@PathVariable String driverId) {
        try {
            return ResponseEntity.ok(bookingService.getDriverActiveBookings(driverId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/bookings/driver/{driverId}/history — Driver ki completed bookings
    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<?> getDriverHistory(@PathVariable String driverId) {
        try {
            return ResponseEntity.ok(bookingService.getDriverCompletedBookings(driverId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    
}