package com.example.translator.service;

import com.example.translator.dto.RegistrationForm;
import com.example.translator.model.User;
import com.example.translator.repository.UserRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Business logic for users: registering and finding them. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection: Spring passes in the objects we need.
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegistrationForm form) {
        // Clean the input: emails are stored in lower case, VIT IDs in upper case.
        String email = form.getEmail().trim().toLowerCase();
        String vitId = form.getVitId().trim().toUpperCase();
        String name = form.getName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("email", "This email is already registered.");
        }
        if (userRepository.existsByVitId(vitId)) {
            throw new DuplicateUserException("vitId", "This VIT ID is already registered.");
        }

        // Hash the password with BCrypt. The plain password is never saved.
        String hashedPassword = passwordEncoder.encode(form.getPassword());
        User user = new User(vitId, name, email, hashedPassword);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Safety net: two people registering at the same moment can pass the
            // checks above, but the database UNIQUE constraint still rejects one.
            throw new DuplicateUserException("email",
                    "This email or VIT ID is already registered.");
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
}
