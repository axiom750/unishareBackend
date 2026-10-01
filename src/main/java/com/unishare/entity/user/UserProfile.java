package com.unishare.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_profiles",
        indexes = {
                @Index(name = "idx_user_profile_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(length = 500)
    private String bio;

    @Column
    private String profilePictureUrl;

    @Column
    private String profilePicturePublicId;

    @Column(length = 200)
    private String universityName;

    @Column
    private LocalDate dateOfBirth;
}