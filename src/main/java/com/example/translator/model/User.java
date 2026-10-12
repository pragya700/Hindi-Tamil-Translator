package com.example.translator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity = a Java class that is mapped to a database table.
 * Each User object is one row in the "users" table.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_vit_id", columnNames = "vit_id")
    }
)
public class User {

    // Primary key. IDENTITY = MySQL AUTO_INCREMENT generates the value.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vit_id", nullable = false, length = 30)
    private String vitId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    // A BCrypt hash is 60 characters long. We never store the real password.
    @Column(nullable = false, length = 100)
    private String password;

    // JPA needs a no-argument constructor.
    public User() {
    }

    public User(String vitId, String name, String email, String password) {
        this.vitId = vitId;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVitId() {
        return vitId;
    }

    public void setVitId(String vitId) {
        this.vitId = vitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
