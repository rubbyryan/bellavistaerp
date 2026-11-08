package com.replica.bellavistaerp.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByStaffNumberIgnoreCase(String staffNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStaffNumberIgnoreCase(String staffNumber);
}
