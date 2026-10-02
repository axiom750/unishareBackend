package com.unishare.utils.scanner;


import com.unishare.annotation.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * Scans application controller methods for @Permission annotations.
 * 
 * Explicitly depends on the application's RequestMappingHandlerMapping,
 * NOT the Actuator's controllerEndpointHandlerMapping.
 */
@Slf4j
@Component
public class PermissionScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final JpaEntityMetadataResolver entityMetadataResolver;

    /**
     * Constructor with explicit qualifier for RequestMappingHandlerMapping.
     * 
     * Spring Boot creates two RequestMappingHandlerMapping beans:
     * 1. requestMappingHandlerMapping - application controllers (THIS ONE)
     * 2. controllerEndpointHandlerMapping - Actuator endpoints (NOT THIS ONE)
     * 
     * We explicitly inject the application controller mapping to scan
     * only application @Permission annotations, not Actuator endpoints.
     */
    public PermissionScanner(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            JpaEntityMetadataResolver entityMetadataResolver
    ) {
        this.handlerMapping = handlerMapping;
        this.entityMetadataResolver = entityMetadataResolver;
    }

    public List<PermissionDefinition> scan() {

        Map<UUID, PermissionDefinition> discovered = new LinkedHashMap<>();

        handlerMapping
                .getHandlerMethods()
                .values()
                .forEach(handlerMethod ->
                        processHandlerMethod(handlerMethod, discovered)
                );

        log.info(
                "[SECURITY] Permission scan completed | discovered={}",
                discovered.size()
        );

        return List.copyOf(discovered.values());
    }

    private void processHandlerMethod(
            HandlerMethod handlerMethod,
            Map<UUID, PermissionDefinition> discovered
    ) {

        Permission permission =
                handlerMethod.getMethodAnnotation(Permission.class);

        if (permission == null) {
            return;
        }

        PermissionDefinition definition =
                buildDefinition(permission, handlerMethod);

        register(discovered, definition, handlerMethod);
    }

    private PermissionDefinition buildDefinition(Permission permission, HandlerMethod handlerMethod) {

        // Validate and parse UUID
        UUID permissionId = parseAndValidateUuid(
                permission.id(),
                handlerMethod
        );

        // Validate displayName
        if (permission.displayName() == null || permission.displayName().isBlank()) {
            throw new IllegalStateException(
                    "Permission displayName cannot be blank | method=" + handlerMethod
            );
        }

        // Resolve base entity table name
        String baseEntity;
        try {
            baseEntity = entityMetadataResolver.resolveTableName(permission.baseEntity());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to resolve baseEntity table name | entity="
                            + permission.baseEntity().getName()
                            + " | method=" + handlerMethod,
                    e
            );
        }

        // Resolve reachable entities table names
        List<String> reachableEntities;
        try {
            reachableEntities = Arrays.stream(permission.reachableEntities())
                    .map(entityClass -> {
                        try {
                            return entityMetadataResolver.resolveTableName(entityClass);
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    "Failed to resolve reachableEntity table name | entity="
                                            + entityClass.getName()
                                            + " | method=" + handlerMethod,
                                    e
                            );
                        }
                    })
                    .distinct()
                    .toList();
        } catch (IllegalStateException e) {
            throw e; // Re-throw with existing message
        }

        return new PermissionDefinition(
                permissionId,
                permission.displayName().trim(),
                permission.description().trim(),
                baseEntity,
                reachableEntities
        );
    }

    private UUID parseAndValidateUuid(String value, HandlerMethod handlerMethod) {

        UUID uuid;

        try {
            uuid = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Invalid permission UUID on controller method: "
                            + handlerMethod,
                    exception
            );
        }

        if (uuid.version() != 7) {
            throw new IllegalStateException(
                    "Permission UUID must be UUID v7: " + value
                            + " | method=" + handlerMethod
            );
        }

        return uuid;
    }

    private void register(
            Map<UUID, PermissionDefinition> discovered,
            PermissionDefinition definition,
            HandlerMethod handlerMethod
    ) {

        PermissionDefinition existing = discovered.putIfAbsent(
                        definition.id(),
                        definition
                );

        if (existing == null) {
            return;
        }

        if (!existing.equals(definition)) {

            throw new IllegalStateException(
                    "Conflicting @Permission definitions for UUID "
                            + definition.id()
                            + " | method="
                            + handlerMethod
            );
        }
    }
}