
package com.unishare.repository.rbac;

import com.unishare.entity.rbac.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("""
    SELECT DISTINCT r
    FROM User u
    JOIN u.roles r
    WHERE u.id = :userId
    """)
    List<Role> findRolesByUserId(@Param("userId") Long userId);
}
