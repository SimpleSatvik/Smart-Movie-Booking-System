package com.moviebooking.repository;

import com.moviebooking.entity.EmailOtp;
import com.moviebooking.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    /**
     * Finds the latest active (unused) OTP for a given email and purpose.
     */
    Optional<EmailOtp> findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            String email,
            OtpPurpose purpose
    );
}
