package com.example.translator.controller;

import com.example.translator.dto.RegistrationForm;
import com.example.translator.service.DuplicateUserException;
import com.example.translator.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Handles the registration and login pages.
 * The actual login check (POST /login) and logout (POST /logout) are done by
 * Spring Security, configured in SecurityConfig.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register"; // templates/register.html
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("form") RegistrationForm form,
                               BindingResult result) {

        // Check that both password fields match
        if (form.getPassword() != null
                && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())
                && !result.hasFieldErrors("confirmPassword")) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }

        // If any validation failed, show the form again with the error messages
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(form);
        } catch (DuplicateUserException e) {
            result.rejectValue(e.getField(), "duplicate", e.getMessage());
            return "register";
        }

        // Success: go to the login page (it shows a success message)
        return "redirect:/login?registered";
    }
}
