package com.unishare.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String githubId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Roles role;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column
    private String userBio;

    @Column
    private String userProfilePictureURL;

    @Column
    private String profilePicturePublicId;

    @Column
    private String universityName;
    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String securityQuestion;

    @JsonIgnore
    @Column
    private String securityAnswer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        role = Roles.USER;
        active = true;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}