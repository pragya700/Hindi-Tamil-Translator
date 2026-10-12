package com.example.translator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.translator.dto.RegistrationForm;
import com.example.translator.model.User;
import com.example.translator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests with a fake (mock) repository: no MySQL needed. */
class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    private RegistrationForm validForm() {
        RegistrationForm form = new RegistrationForm();
        form.setVitId("24mca0001");
        form.setName("  Asha Kumar ");
        form.setEmail("  Asha@Example.COM ");
        form.setPassword("secret123");
        form.setConfirmPassword("secret123");
        return form;
    }

    @Test
    void registerHashesPasswordAndNormalizesFields() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(validForm());

        assertEquals("24MCA0001", saved.getVitId());
        assertEquals("Asha Kumar", saved.getName());
        assertEquals("asha@example.com", saved.getEmail());
        assertNotEquals("secret123", saved.getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        DuplicateUserException ex =
                assertThrows(DuplicateUserException.class, () -> userService.register(validForm()));

        assertEquals("email", ex.getField());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicateVitIdIsRejected() {
        when(userRepository.existsByVitId("24MCA0001")).thenReturn(true);

        DuplicateUserException ex =
                assertThrows(DuplicateUserException.class, () -> userService.register(validForm()));

        assertEquals("vitId", ex.getField());
        verify(userRepository, never()).save(any(User.class));
    }
}
