package com.example.demo.bookingrepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.bookingentity.Booking;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByStatusOrderByCreatedAtDesc(Booking.BookingStatus status);

    List<Booking> findByDriver_DriverIdOrderByCreatedAtDesc(String driverId);

    // Admin: screenshot upload wali pending bookings
    List<Booking> findByPaymentStatusOrderByCreatedAtDesc(String paymentStatus);

    // Admin dashboard stats
    long countByStatus(Booking.BookingStatus status);

    long countByPaymentStatus(String paymentStatus);

    // Total commission earned (sirf VERIFIED payments)
    @Query("SELECT COALESCE(SUM(b.commissionAmount), 0) FROM Booking b WHERE b.paymentStatus = 'VERIFIED'")
    Double getTotalCommissionEarned();

    // All bookings for admin - latest first
    List<Booking> findAllByOrderByCreatedAtDesc();
}