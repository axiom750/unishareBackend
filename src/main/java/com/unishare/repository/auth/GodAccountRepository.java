package com.unishare.repository.auth;

import com.unishare.entity.rbac.GodAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GodAccount entity.
 * 
 * GodAccount is the root control-plane account.
 * There must be exactly ONE GodAccount in the system.
 */
public interface GodAccountRepository extends JpaRepository<GodAccount, UUID> {

    /**
     * Find GodAccount by username.
     * 
     * @param username The GodAccount username
     * @return Optional containing GodAccount if found
     */
    Optional<GodAccount> findByUsername(String username);

    /**
     * Find GodAccount by email.
     * 
     * @param email The GodAccount email
     * @return Optional containing GodAccount if found
     */
    Optional<GodAccount> findByEmail(String email);

    /**
     * Check if a GodAccount exists with the given username.
     * 
     * @param username The username to check
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if a GodAccount exists with the given email.
     * 
     * @param email The email to check
     * @return true if exists
     */
    boolean existsByEmail(String email);
}
