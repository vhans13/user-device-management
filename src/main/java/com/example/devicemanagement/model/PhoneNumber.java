package com.example.devicemanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_numbers")
public class PhoneNumber {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false, length = 30)
    private String label;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PhoneNumber() {
    }

    public PhoneNumber(String number, String label, UUID userId) {
        this.id = UUID.randomUUID();
        this.number = number;
        this.label = label;
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getLabel() {
        return label;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
