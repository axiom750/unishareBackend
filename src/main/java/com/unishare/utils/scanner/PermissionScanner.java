package com.unishare.utils.scanner;


import com.unishare.annotation.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans application controller methods for @Permission annotations.
 *
 * Every @Permission handler must also carry
 * {@code @PreAuthorize("hasAuthority('PERMISSION_NAME')")} on the SAME method.
 * That authority is the canonical permission name (Permission.name); it is
 * never declared on @Permission itself.
 *
 * Explicitly depends on the application's RequestMappingHandlerMapping,
 * NOT the Actuator's controllerEndpointHandlerMapping.
 */
@Slf4j
@Component
public class PermissionScanner {

    /**
     * The only supported permission expression: a single hasAuthority('NAME')
     * call, tolerant of whitespace and either quote style. Not a SpEL parser.
     */
    private static final Pattern HAS_AUTHORITY = Pattern.compile(
            "^\\s*hasAuthority\\s*\\(\\s*(['\"])([A-Za-z][A-Za-z0-9_.:-]*)\\1\\s*\\)\\s*$"
    );

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
        Map<String, UUID> idsByName = new HashMap<>();

        handlerMapping
                .getHandlerMethods()
                .values()
                .forEach(handlerMethod ->
                        processHandlerMethod(handlerMethod, discovered, idsByName)
                );

        log.info(
                "[SECURITY] Permission scanning completed | discovered={}",
                discovered.size()
        );

        return List.copyOf(discovered.values());
    }

    private void processHandlerMethod(
            HandlerMethod handlerMethod,
            Map<UUID, PermissionDefinition> discovered,
            Map<String, UUID> idsByName
    ) {

        Permission permission =
                handlerMethod.getMethodAnnotation(Permission.class);

        if (permission == null) {
            return;
        }

        PermissionDefinition definition =
                buildDefinition(permission, handlerMethod);

        register(discovered, definition, handlerMethod);

        UUID firstId = idsByName.putIfAbsent(definition.name(), definition.id());

        if (firstId != null && !firstId.equals(definition.id())) {
            throw new IllegalStateException(
                    "[SECURITY] Duplicate permission name | name=" + definition.name()
                            + " | firstId=" + firstId
                            + " | secondId=" + definition.id()
                            + " | method=" + describe(handlerMethod)
            );
        }
    }

    /**
     * Extracts the canonical permission name from the SAME handler method's
     * {@code @PreAuthorize("hasAuthority('NAME')")}. Missing or unsupported
     * expressions fail startup; a name is never guessed or generated.
     */
    private String extractPermissionName(HandlerMethod handlerMethod, String permissionId) {

        PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);

        if (preAuthorize == null) {
            throw invalidPreAuthorize(handlerMethod, permissionId, null,
                    "@Permission requires @PreAuthorize(\"hasAuthority('PERMISSION_NAME')\") on the same method");
        }

        Matcher matcher = HAS_AUTHORITY.matcher(preAuthorize.value());

        if (!matcher.matches()) {
            throw invalidPreAuthorize(handlerMethod, permissionId, preAuthorize.value(),
                    "unsupported expression; only hasAuthority('PERMISSION_NAME') defines a permission name");
        }

        return matcher.group(2);
    }

    private IllegalStateException invalidPreAuthorize(
            HandlerMethod handlerMethod,
            String permissionId,
            String expression,
            String reason
    ) {
        return new IllegalStateException(
                "[SECURITY] Invalid permission declaration | method=" + describe(handlerMethod)
                        + " | permissionId=" + permissionId
                        + " | preAuthorize=" + (expression == null ? "<missing>" : "\"" + expression + "\"")
                        + " | reason=" + reason
        );
    }

    private static String describe(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }

    private PermissionDefinition buildDefinition(Permission permission, HandlerMethod handlerMethod) {

        // Validate and parse UUID
        UUID permissionId = parseAndValidateUuid(
                permission.id(),
                handlerMethod
        );

        // Canonical name from @PreAuthorize on the same method
        String name = extractPermissionName(handlerMethod, permission.id());

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
                name,
                permission.displayName().trim(),
                permission.description().trim(),
                baseEntity,
                reachableEntities,
                permission.domain()
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