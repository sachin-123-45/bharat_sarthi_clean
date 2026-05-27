package com.example.demo.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.bookingentity.Admin;
import com.example.demo.bookingentity.Booking;
import com.example.demo.bookingentity.Driver;
import com.example.demo.bookingrepository.AdminRepository;
import com.example.demo.bookingrepository.BookingRepository;
import com.example.demo.bookingrepository.DriverRepository;
import com.example.demo.dtos.BookingDto;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private DriverRepository driverRepo;

    // ── NEW: Admin notifications ke liye AdminRepository inject kiya ──
    @Autowired
    private AdminRepository adminRepo;

    private final Random random = new Random();

    // Screenshot save karne ki folder
    private final String UPLOAD_DIR = "uploads/payment-screenshots/";

    // ── Create Booking ───────────────────────────────────────────────
    @Transactional
    public BookingDto.Response createBooking(BookingDto.CreateRequest req)
            throws FirebaseMessagingException {

        Booking b = new Booking();

        b.setId(generateBookingId());
        b.setPickupLocation(req.getPickupLocation());
        b.setDropLocation(req.getDropLocation());
        b.setStartDate(req.getStartDate());
        b.setEndDate(req.getEndDate());
        b.setCustomerMobile(req.getCustomerMobile());
        b.setCustomerFcmToken(req.getCustomerFcmToken());

        b.setStatus(Booking.BookingStatus.PENDING);
        b.setCreatedAt(LocalDateTime.now());

        bookingRepo.save(b);

        // 🔥 Saare online drivers ko notification
        List<Driver> drivers = driverRepo.findAll();

        for (Driver driver : drivers) {

            String token = driver.getFcmToken();

            if (token != null && !token.isEmpty()) {

                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(
                                Notification.builder()
                                        .setTitle("नई बुकिंग")
                                        .setBody("नई सवारी आई है")
                                        .build()
                        )
                        .build();

                FirebaseMessaging.getInstance().send(message);
            }
        }

        return toResponse(b);
    }

    // ── Get Booking by ID ────────────────────────────────────────────
    public BookingDto.Response getBooking(String id) {

        Booking b = bookingRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found: " + id));

        return toResponse(b);
    }

    // ── Get All Pending Bookings (for drivers) ───────────────────────
    public List<BookingDto.Response> getPendingBookings() {

        return bookingRepo
                .findByStatusOrderByCreatedAtDesc(
                        Booking.BookingStatus.PENDING
                )
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Accept Booking (first driver wins) ──────────────────────────
    @Transactional
    public synchronized BookingDto.Response acceptBooking(
            String bookingId,
            String driverId
    ) throws FirebaseMessagingException {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found: " + bookingId));

        if (b.getStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException(
                    "Booking already " + b.getStatus() +
                            ". Kisi aur ne le li!"
            );
        }

        Driver driver = driverRepo.findByDriverId(driverId)
                .orElseThrow(() ->
                        new RuntimeException("Driver not found: " + driverId));

        b.setStatus(Booking.BookingStatus.ACCEPTED);
        b.setDriver(driver);
        b.setAcceptedAt(LocalDateTime.now());

        bookingRepo.save(b);

        // 🔥 Customer ko notification — driver mil gaya
        String customerToken = b.getCustomerFcmToken();

        if (customerToken != null && !customerToken.isEmpty()) {

            Message message = Message.builder()
                    .setToken(customerToken)
                    .setNotification(
                            Notification.builder()
                                    .setTitle("ड्राइवर मिल गया")
                                    .setBody(
                                            driver.getName() +
                                                    " ने आपकी बुकिंग स्वीकार कर ली"
                                    )
                                    .build()
                    )
                    .build();

            FirebaseMessaging.getInstance().send(message);
        }

        return toResponse(b);
    }

    // ── Cancel Booking ───────────────────────────────────────────────
    @Transactional
    public void cancelBooking(String bookingId) {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));

        if (b.getStatus() == Booking.BookingStatus.PENDING) {

            b.setStatus(Booking.BookingStatus.CANCELLED);

            bookingRepo.save(b);
        }
    }

    // ── STEP 1: Total fare set karo ──────────────────────────────────
    @Transactional
    public BookingDto.Response setTotalAndCalculateAdvance(
            String bookingId,
            Double totalAmount
    ) {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found: " + bookingId));

        double advance =
                Math.round(totalAmount * 0.15 * 100.0) / 100.0;

        double remaining =
                Math.round((totalAmount - advance) * 100.0) / 100.0;

        double commission = advance;

        b.setTotalAmount(totalAmount);
        b.setAdvanceAmount(advance);
        b.setRemainingCashAmount(remaining);
        b.setCommissionAmount(commission);

        b.setPaymentStatus("PENDING");

        bookingRepo.save(b);

        return toResponse(b);
    }

    // ── STEP 2: Upload Screenshot + Admin ko notification ───────────
    //
    // CHANGE: File save hone ke baad saare active admins ko
    //         FCM push notification bheja jata hai.
    //         Agar kisi admin ka token null/empty hai to skip karo.
    //         FirebaseMessagingException ko catch karo — ek admin ka
    //         failed token baaki admins ki notification nahi rokta.
    //
    @Transactional
    public BookingDto.Response uploadPaymentScreenshot(
            String bookingId,
            MultipartFile screenshot
    ) throws IOException {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found: " + bookingId));

        // ── File save karo ───────────────────────────────────────────
        File uploadDir = new File(UPLOAD_DIR);

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalName = screenshot.getOriginalFilename();

        String extension =
                originalName != null && originalName.contains(".")
                        ? originalName.substring(
                        originalName.lastIndexOf("."))
                        : ".jpg";

        String fileName =
                bookingId + "_" +
                        UUID.randomUUID().toString() +
                        extension;

        Path filePath = Paths.get(UPLOAD_DIR + fileName);

        Files.write(filePath, screenshot.getBytes());

        b.setPaymentScreenshot(filePath.toString());
        b.setPaymentStatus("SCREENSHOT_UPLOADED");

        bookingRepo.save(b);

        // ── NEW: Saare active admins ko push notification bhejo ──────
        sendPaymentNotificationToAdmins(bookingId);

        return toResponse(b);
    }

    // ── STEP 3: Verify Payment ───────────────────────────────────────
    @Transactional
    public BookingDto.Response verifyPayment(String bookingId) {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found: " + bookingId));

        b.setPaymentStatus("VERIFIED");
        b.setStatus(Booking.BookingStatus.COMPLETED);

        bookingRepo.save(b);

        return toResponse(b);
    }

    // ── Driver Complete Ride ─────────────────────────────────────────
    @Transactional
    public BookingDto.Response completeRide(
            String bookingId,
            String driverId
    ) {

        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found: " + bookingId));

        if (b.getDriver() == null ||
                !b.getDriver().getDriverId().equals(driverId)) {

            throw new RuntimeException(
                    "Aap is booking ke driver nahi hain");
        }

        if (b.getStatus() != Booking.BookingStatus.ACCEPTED) {

            throw new RuntimeException(
                    "Sirf ACCEPTED booking complete ho sakti hai");
        }

        b.setStatus(Booking.BookingStatus.COMPLETED);

        bookingRepo.save(b);

        return toResponse(b);
    }

    // ── Driver Active Bookings ───────────────────────────────────────
    public List<BookingDto.Response> getDriverActiveBookings(
            String driverId
    ) {

        return bookingRepo
                .findByDriver_DriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .filter(b ->
                        b.getStatus() ==
                                Booking.BookingStatus.ACCEPTED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Driver Completed Bookings ────────────────────────────────────
    public List<BookingDto.Response> getDriverCompletedBookings(
            String driverId
    ) {

        return bookingRepo
                .findByDriver_DriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .filter(b ->
                        b.getStatus() ==
                                Booking.BookingStatus.COMPLETED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin Pending Screenshots ────────────────────────────────────
    public List<BookingDto.Response> getPendingScreenshotBookings() {

        return bookingRepo
                .findByPaymentStatusOrderByCreatedAtDesc(
                        "SCREENSHOT_UPLOADED")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── NEW PRIVATE HELPER: Admin FCM Notification ───────────────────
    // Saare active admins ko payment notification bhejo.
    // Har admin ke liye alag try-catch — ek ka failure baaki ko nahi rokta.
    private void sendPaymentNotificationToAdmins(String bookingId) {

        List<Admin> activeAdmins = adminRepo.findAllByActiveTrue();

        for (Admin admin : activeAdmins) {

            String token = admin.getFcmToken();

            if (token == null || token.isEmpty()) {
                // Is admin ka FCM token save nahi hai — skip karo
                continue;
            }

            try {
                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(
                                Notification.builder()
                                        .setTitle("नई Payment Request")
                                        .setBody("Ek customer ne payment screenshot upload kiya hai")
                                        .build()
                        )
                        .build();

                FirebaseMessaging.getInstance().send(message);

            } catch (FirebaseMessagingException e) {
                // Log karo lekin exception propagate mat karo —
                // notification fail hona payment upload ko nahi rokna chahiye
                System.err.println(
                        "Admin FCM notification fail: adminId=" +
                        admin.getId() + ", bookingId=" + bookingId +
                        ", error=" + e.getMessage()
                );
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private String generateBookingId() {

        String id;

        do {
            id = "BSD-" + (1000 + random.nextInt(9000));
        }
        while (bookingRepo.existsById(id));

        return id;
    }

    private BookingDto.Response toResponse(Booking b) {

        BookingDto.Response r = new BookingDto.Response();

        r.setId(b.getId());
        r.setPickupLocation(b.getPickupLocation());
        r.setDropLocation(b.getDropLocation());
        r.setStartDate(b.getStartDate());
        r.setEndDate(b.getEndDate());

        r.setCustomerMobile(b.getCustomerMobile());

        r.setStatus(b.getStatus().name());

        r.setPaymentStatus(b.getPaymentStatus());

        r.setTotalAmount(b.getTotalAmount());

        r.setAdvanceAmount(b.getAdvanceAmount());

        r.setRemainingCashAmount(b.getRemainingCashAmount());

        if (b.getDriver() != null) {

            Driver d = b.getDriver();

            BookingDto.Response.DriverInfo di =
                    new BookingDto.Response.DriverInfo();

            di.setDriverId(d.getDriverId());
            di.setName(d.getName());
            di.setMobile(d.getMobile());
            di.setAddress(d.getAddress());
            di.setAadhaar(d.getAadhaarNumber());
            di.setLicence(d.getLicenceNumber());

            if (d.getPhotoPath() != null) {

                di.setPhotoUrl(
                        "/api/drivers/" +
                                d.getDriverId() +
                                "/photo"
                );
            }

            r.setDriver(di);
        }

        return r;
    }
}