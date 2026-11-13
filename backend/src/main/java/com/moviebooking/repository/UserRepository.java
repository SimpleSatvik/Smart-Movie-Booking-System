package com.moviebooking.repository;

import com.moviebooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA automatically writes the SQL:
    // "SELECT * FROM users WHERE gmail = ? AND is_deleted = false"
    Optional<User> findByGmailAndIsDeletedFalse(String gmail);

    Optional<User> findByGmailIgnoreCase(String gmail);

    boolean existsByGmail(String gmail);

    boolean existsByGmailIgnoreCase(String gmail);

    long countByIsDeletedFalse();

    List<User> findByIsDeletedFalseOrderByIdAsc();
}
