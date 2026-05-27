package com.example.demo.bookingrepository;

import com.example.demo.bookingentity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsernameAndActiveTrue(String username);

    // ── NEW: Saare active admins dhundho (FCM broadcast ke liye) ─────
    List<Admin> findAllByActiveTrue();
}