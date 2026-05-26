package com.example.demo.bookingrepository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.bookingentity.Driver;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUsernameAndActiveTrue(String username);
    Optional<Driver> findByDriverId(String driverId);
    List<Driver> findByOnlineTrueAndActiveTrue();
}