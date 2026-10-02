package com.unishare.utils.scanner;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves actual JPA/Hibernate table names from entity classes.
 * Uses standard JPA metadata APIs for maximum compatibility.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaEntityMetadataResolver {

    private final EntityManager entityManager;

    /**
     * Resolves the physical table name for a JPA entity class.
     * 
     * Uses JPA @Table annotation and naming strategy fallback.
     *
     * @param entityClass The JPA entity class
     * @return The physical table name
     * @throws IllegalStateException if the class is not a JPA entity
     */
    public String resolveTableName(Class<?> entityClass) {
        
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }

        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalStateException(
                    "Permission references a class that is not a JPA entity: "
                            + entityClass.getName()
            );
        }

        try {
            // Verify entity is known to JPA
            EntityType<?> entityType = entityManager.getMetamodel().entity(entityClass);
            
            // First try: Check for @Table annotation
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                String tableName = tableAnnotation.name();
                log.debug("[SCANNER] Resolved table name from @Table: {} -> {}", 
                        entityClass.getSimpleName(), tableName);
                return tableName;
            }

            // Second try: Use entity name (JPA default is class name)
            String entityName = entityType.getName();
            
            // Apply standard JPA naming convention: convert to lowercase
            // Most JPA providers default to lowercase table names
            String tableName = toSnakeCase(entityName);
            
            log.debug("[SCANNER] Resolved table name using naming convention: {} -> {}", 
                    entityClass.getSimpleName(), tableName);
            
            return tableName;

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Class is not a JPA entity: " + entityClass.getName(), e
            );
        }
    }

    /**
     * Validates that a class is a JPA entity.
     *
     * @param entityClass The class to validate
     * @return true if it's a valid JPA entity
     */
    public boolean isJpaEntity(Class<?> entityClass) {
        return entityClass != null && entityClass.isAnnotationPresent(Entity.class);
    }

    /**
     * Convert camelCase or PascalCase to snake_case (common JPA naming strategy).
     * Example: UserProfile -> user_profile
     *
     * @param name The name to convert
     * @return snake_case version
     */
    private String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // Insert underscore before uppercase letters and convert to lowercase
        String result = name.replaceAll("([a-z])([A-Z])", "$1_$2")
                            .toLowerCase();
        
        return result;
    }
}
