package com.tainted.authuser.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_account",
       uniqueConstraints = @UniqueConstraint(columnNames = {"social_provider", "external_id"}))
public class UserAccount {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "social_provider", nullable = false, length = 32)
    private String socialProvider;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String id, String socialProvider, String externalId, Instant createdAt) {
        this.id = id;
        this.socialProvider = socialProvider;
        this.externalId = externalId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSocialProvider() { return socialProvider; }
    public String getExternalId() { return externalId; }
    public Instant getCreatedAt() { return createdAt; }
}
