package com.example.translator.repository;

import com.example.translator.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA writes the implementation of this interface for us.
 * The method names are turned into SQL queries automatically.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByVitId(String vitId);
}
