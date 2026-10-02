package com.unishare.entity.auth;

import com.unishare.enums.auth.PermissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String baseEntity;

    @Column(length = 1000)
    private String reachableEntities;  // Comma-separated list of table names

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PermissionStatus status = PermissionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastSeenAt;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get reachableEntities as a Set for convenient access.
     * 
     * @return Set of entity table names, empty set if null or empty
     */
    public Set<String> getReachableEntitiesAsSet() {
        if (reachableEntities == null || reachableEntities.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(reachableEntities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Set reachableEntities from a Collection.
     * Stores as comma-separated string.
     * 
     * @param entities Collection of entity table names
     */
    public void setReachableEntitiesFromCollection(Collection<String> entities) {
        if (entities == null || entities.isEmpty()) {
            this.reachableEntities = "";
        } else {
            this.reachableEntities = String.join(",", entities);
        }
    }
}