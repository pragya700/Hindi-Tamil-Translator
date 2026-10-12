package com.example.translator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Security rules of the application:
 * - who can open which URL
 * - how login and logout work
 * CSRF protection stays ON (the default), so every POST needs a CSRF token.
 */
@Configuration
public class SecurityConfig {

    // Used both for hashing passwords at registration and checking them at login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages and static files
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/error").permitAll()
                // Everything else (/home, /api/translate ...) needs login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")                 // our own login page
                .loginProcessingUrl("/login")        // Spring Security handles POST /login
                .usernameParameter("email")          // the form field is called "email"
                .passwordParameter("password")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                // POST /logout (CSRF protected)
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // For API calls without login, answer 401 instead of redirecting to the login page.
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api/"))
            );

        return http.build();
    }
}
