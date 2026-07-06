package com.labortrack.labortrack_backend.user.repository;

import com.labortrack.labortrack_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check weather user email already exists in db.
     */
    boolean existsByEmail(String email);
}
