package com.example.demo.Services;

import com.example.demo.bookingentity.Admin;
import com.example.demo.bookingentity.Booking;
import com.example.demo.bookingentity.Driver;
import com.example.demo.bookingrepository.AdminRepository;
import com.example.demo.bookingrepository.BookingRepository;
import com.example.demo.bookingrepository.DriverRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired private AdminRepository adminRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private DriverRepository driverRepo;

    // ── Admin Login (no registration — only DB insert by owner) ──────
    public Map<String, Object> login(String username, String password) {
        Admin admin = adminRepo.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("Admin nahi mila ya account inactive hai"));

        if (!admin.getPassword().equals(password)) {
            throw new RuntimeException("Galat password");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminRepo.save(admin);

        Map<String, Object> res = new HashMap<>();
        res.put("adminId", admin.getId());
        res.put("username", admin.getUsername());
        res.put("fullName", admin.getFullName());
        // Simple token — production mein JWT use karein
        res.put("token", "ADMIN_" + admin.getId() + "_" + System.currentTimeMillis());
        return res;
    }

    // ── Dashboard Statistics ──────────────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalBookings     = bookingRepo.count();
        long pendingBookings   = bookingRepo.countByStatus(Booking.BookingStatus.PENDING);
        long acceptedBookings  = bookingRepo.countByStatus(Booking.BookingStatus.ACCEPTED);
        long completedBookings = bookingRepo.countByStatus(Booking.BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepo.countByStatus(Booking.BookingStatus.CANCELLED);
        long pendingPayments   = bookingRepo.countByPaymentStatus("SCREENSHOT_UPLOADED");
        long totalDrivers      = driverRepo.count();
        Double totalCommission = bookingRepo.getTotalCommissionEarned();

        stats.put("totalBookings",     totalBookings);
        stats.put("pendingBookings",   pendingBookings);
        stats.put("acceptedBookings",  acceptedBookings);
        stats.put("completedBookings", completedBookings);
        stats.put("cancelledBookings", cancelledBookings);
        stats.put("pendingPayments",   pendingPayments);
        stats.put("totalDrivers",      totalDrivers);
        stats.put("totalCommission",   totalCommission != null ? totalCommission : 0.0);

        return stats;
    }

    // ── All Bookings for Admin ────────────────────────────────────────
    public List<Map<String, Object>> getAllBookings() {
        return bookingRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toAdminBookingMap).collect(Collectors.toList());
    }

    // ── Pending Screenshot Bookings ───────────────────────────────────
    public List<Map<String, Object>> getPendingPaymentBookings() {
        return bookingRepo.findByPaymentStatusOrderByCreatedAtDesc("SCREENSHOT_UPLOADED")
                .stream().map(this::toAdminBookingMap).collect(Collectors.toList());
    }

    // ── Approve Payment ───────────────────────────────────────────────
    // Customer ne advance pay kar diya → Admin verify karta hai
    // paymentStatus: SCREENSHOT_UPLOADED → VERIFIED
    // bookingStatus: Unchanged (stays ACCEPTED — driver already assigned)
    // Customer ko: ride confirmed message dikhega (frontend polls VERIFIED)
    @Transactional
    public Map<String, Object> approvePayment(String bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking nahi mili: " + bookingId));

        if (!"SCREENSHOT_UPLOADED".equals(b.getPaymentStatus())) {
            throw new RuntimeException("Yeh booking verify ke liye ready nahi hai. Current status: " + b.getPaymentStatus());
        }

        // Payment verify kar do — booking status mat badlo (driver ne already accept kiya)
        b.setPaymentStatus("VERIFIED");
        // Note: b.status stays ACCEPTED — trip abhi complete nahi hui
        // Jab driver trip complete kare tab COMPLETED hoga
        bookingRepo.save(b);

        Map<String, Object> res = new HashMap<>();
        res.put("message",       "Payment approve ho gayi! Customer ko ride confirmed message milega.");
        res.put("bookingId",     bookingId);
        res.put("paymentStatus", "VERIFIED");
        res.put("bookingStatus", b.getStatus().name());
        return res;
    }

    // ── Reject Payment ────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> rejectPayment(String bookingId, String reason) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking nahi mili: " + bookingId));

        b.setPaymentStatus("REJECTED");
        b.setRejectionReason(reason);
        // Booking status ACCEPTED raho — customer dobara try kar sakta hai
        bookingRepo.save(b);

        Map<String, Object> res = new HashMap<>();
        res.put("message",       "Payment reject kar di gayi. Customer ko galat payment message dikhega.");
        res.put("bookingId",     bookingId);
        res.put("paymentStatus", "REJECTED");
        res.put("reason",        reason);
        return res;
    }

    // ── All Drivers for Admin ─────────────────────────────────────────
    public List<Map<String, Object>> getAllDrivers() {
        return driverRepo.findAll().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("driverId",      d.getDriverId());
            m.put("name",          d.getName());
            m.put("mobile",        d.getMobile());
            m.put("address",       d.getAddress());
            m.put("licenceNumber", d.getLicenceNumber());
            m.put("online",        d.isOnline());
            m.put("active",        d.isActive());
            m.put("createdAt",     d.getCreatedAt());
            m.put("lastSeen",      d.getLastSeen());
            // Booking count for this driver
            long bookingCount = bookingRepo.findByDriver_DriverIdOrderByCreatedAtDesc(d.getDriverId()).size();
            m.put("totalBookings", bookingCount);
            // Commission earned by this driver's bookings (admin commission, not driver earnings)
            double driverBookingCommission = bookingRepo
                    .findByDriver_DriverIdOrderByCreatedAtDesc(d.getDriverId())
                    .stream()
                    .filter(b -> "VERIFIED".equals(b.getPaymentStatus()) && b.getCommissionAmount() != null)
                    .mapToDouble(b -> b.getCommissionAmount())
                    .sum();
            m.put("commissionGenerated", driverBookingCommission);
            return m;
        }).collect(Collectors.toList());
    }

    // ── Block / Unblock Driver ────────────────────────────────────────
    @Transactional
    public Map<String, Object> toggleDriverBlock(String driverId, boolean block) {
        Driver d = driverRepo.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver nahi mila: " + driverId));
        d.setActive(!block);
        if (block) d.setOnline(false); // Block hone par offline kar do
        driverRepo.save(d);

        Map<String, Object> res = new HashMap<>();
        res.put("driverId", driverId);
        res.put("active",   !block);
        res.put("message",  block ? "Driver " + driverId + " block kar diya" : "Driver " + driverId + " unblock kar diya");
        return res;
    }

    // ── Helper: Booking to Admin Map ──────────────────────────────────
    // commissionAmount yahan include hai — sirf admin dashboard pe dikhega
    private Map<String, Object> toAdminBookingMap(Booking b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",                 b.getId());
        m.put("customerMobile",     b.getCustomerMobile());
        m.put("pickupLocation",     b.getPickupLocation());
        m.put("dropLocation",       b.getDropLocation());
        m.put("startDate",          b.getStartDate());
        m.put("endDate",            b.getEndDate());
        m.put("status",             b.getStatus().name());
        m.put("paymentStatus",      b.getPaymentStatus());
        m.put("totalAmount",        b.getTotalAmount());
        m.put("advanceAmount",      b.getAdvanceAmount());       // 15% — admin commission
        m.put("remainingCashAmount",b.getRemainingCashAmount()); // 85% — driver cash
        m.put("commissionAmount",   b.getCommissionAmount());    // Admin ko dikhana hai
        m.put("paymentScreenshot",  b.getPaymentScreenshot());
        m.put("rejectionReason",    b.getRejectionReason());
        m.put("createdAt",          b.getCreatedAt());
        m.put("acceptedAt",         b.getAcceptedAt());

        if (b.getDriver() != null) {
            Map<String, Object> drv = new HashMap<>();
            drv.put("driverId", b.getDriver().getDriverId());
            drv.put("name",     b.getDriver().getName());
            drv.put("mobile",   b.getDriver().getMobile());
            m.put("driver", drv);
        }
        return m;
    }
}