package com.unishare.utils.scanner;

import com.unishare.UnishareApplication;
import com.unishare.annotation.GodAccount;
import com.unishare.annotation.GodRole;
import com.unishare.annotation.uniShareRole;
import com.unishare.utils.seeder.SecuritySeedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Scans security declarations from UnishareApplication.class ONLY.
 *
 * No classpath scanning, no bean scanning, no database reads, no writes.
 * The single declaration source is read directly via reflection.
 *
 * Scanned declarations:
 * - @GodAccount   (exactly one)
 * - @GodRole      (exactly one)
 * - @uniShareRole (one or more)
 *
 * Invalid declarations fail fast; nothing is silently repaired.
 */
@Slf4j
@Component
public class SecurityDeclarationScanner {

    static final Class<?> DECLARATION_SOURCE = UnishareApplication.class;

    public SecurityDeclarations scan(SecuritySeedProperties.Scanning scanning) {
        return scan(DECLARATION_SOURCE, scanning);
    }

    /**
     * Package-private so tests can validate fixtures; production code always
     * scans {@link #DECLARATION_SOURCE}.
     */
    SecurityDeclarations scan(Class<?> source, SecuritySeedProperties.Scanning scanning) {

        log.info("[SECURITY] Scanning declarations from: {}", source.getName());

        GodAccountDefinition godAccount = null;
        if (scanning.shouldScanGodAccount()) {
            godAccount = scanGodAccount(source);
        } else {
            log.info("[SECURITY] @GodAccount scanning disabled");
        }

        GodRoleDefinition godRole = null;
        if (scanning.shouldScanGodRole()) {
            godRole = scanGodRole(source);
        } else {
            log.info("[SECURITY] @GodRole scanning disabled");
        }

        if (godAccount != null && godRole != null && godAccount.id().equals(godRole.id())) {
            throw new IllegalStateException(
                    "[SECURITY] @GodAccount id and @GodRole id must differ: " + godRole.id()
            );
        }

        List<uniShareRoleDefinition> roles = List.of();
        if (scanning.shouldScanRoles()) {
            roles = scanRoles(source);
            validateControlPlaneIsolation(godAccount, godRole, roles);
        } else {
            log.info("[SECURITY] @uniShareRole scanning disabled");
        }

        log.info("[SECURITY] Declaration scanning completed");

        return new SecurityDeclarations(godAccount, godRole, roles);
    }

    private GodAccountDefinition scanGodAccount(Class<?> source) {

        GodAccount annotation = requireExactlyOne(source, GodAccount.class);

        GodAccountDefinition definition = new GodAccountDefinition(
                parseUuidV7(annotation.id(), "@GodAccount.id"),
                requireText(annotation.username(), "@GodAccount.username"),
                requireText(annotation.email(), "@GodAccount.email").toLowerCase(Locale.ROOT),
                requireText(annotation.displayName(), "@GodAccount.displayName")
        );

        log.info("[SECURITY] @GodAccount discovered | id={}", definition.id());

        return definition;
    }

    private GodRoleDefinition scanGodRole(Class<?> source) {

        GodRole annotation = requireExactlyOne(source, GodRole.class);

        GodRoleDefinition definition = new GodRoleDefinition(
                parseUuidV7(annotation.id(), "@GodRole.id"),
                requireText(annotation.name(), "@GodRole.name"),
                requireText(annotation.description(), "@GodRole.description")
        );

        log.info("[SECURITY] @GodRole discovered | id={} | name={}",
                definition.id(), definition.name());

        return definition;
    }

    private List<uniShareRoleDefinition> scanRoles(Class<?> source) {

        uniShareRole[] annotations = source.getAnnotationsByType(uniShareRole.class);

        if (annotations.length == 0) {
            throw new IllegalStateException(
                    "[SECURITY] At least one @uniShareRole declaration is required on "
                            + source.getName()
            );
        }

        List<uniShareRoleDefinition> definitions = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();
        Set<String> seenNames = new HashSet<>();

        for (uniShareRole annotation : annotations) {

            uniShareRoleDefinition definition = new uniShareRoleDefinition(
                    parseUuidV7(annotation.id(), "@uniShareRole.id"),
                    requireText(annotation.name(), "@uniShareRole.name"),
                    requireText(annotation.description(), "@uniShareRole.description")
            );

            if (!seenIds.add(definition.id())) {
                throw new IllegalStateException(
                        "[SECURITY] Duplicate @uniShareRole id: " + definition.id()
                );
            }

            if (!seenNames.add(definition.name().toUpperCase(Locale.ROOT))) {
                throw new IllegalStateException(
                        "[SECURITY] Duplicate @uniShareRole name: " + definition.name()
                );
            }

            definitions.add(definition);

            log.info("[SECURITY] @uniShareRole discovered | id={} | name={}",
                    definition.id(), definition.name());
        }

        return definitions;
    }

    /**
     * Normal RBAC roles must never reuse control-plane identity
     * (GodRole id/name, GodAccount id/username).
     */
    private void validateControlPlaneIsolation(
            GodAccountDefinition godAccount,
            GodRoleDefinition godRole,
            List<uniShareRoleDefinition> roles
    ) {

        for (uniShareRoleDefinition role : roles) {

            if (godRole != null && godRole.id().equals(role.id())) {
                throw new IllegalStateException(
                        "[SECURITY] @uniShareRole must not reuse the @GodRole id: " + role.id()
                );
            }

            if (godRole != null && godRole.name().equalsIgnoreCase(role.name())) {
                throw new IllegalStateException(
                        "[SECURITY] @uniShareRole must not use the @GodRole name: " + role.name()
                );
            }

            if (godAccount != null && godAccount.id().equals(role.id())) {
                throw new IllegalStateException(
                        "[SECURITY] @uniShareRole must not reuse the @GodAccount id: " + role.id()
                );
            }

            if (godAccount != null && godAccount.username().equalsIgnoreCase(role.name())) {
                throw new IllegalStateException(
                        "[SECURITY] @uniShareRole must not use the @GodAccount username: " + role.name()
                );
            }
        }
    }

    private <A extends java.lang.annotation.Annotation> A requireExactlyOne(
            Class<?> source,
            Class<A> annotationType
    ) {

        A[] annotations = source.getAnnotationsByType(annotationType);

        if (annotations.length != 1) {
            throw new IllegalStateException(
                    "[SECURITY] Exactly one @" + annotationType.getSimpleName()
                            + " declaration is required on " + source.getName()
                            + " (found " + annotations.length + ")"
            );
        }

        return annotations[0];
    }

    static UUID parseUuidV7(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[SECURITY] " + field + " cannot be blank");
        }

        UUID uuid;

        try {
            uuid = UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "[SECURITY] " + field + " is not a valid UUID: " + value,
                    exception
            );
        }

        if (uuid.version() != 7 || uuid.variant() != 2) {
            throw new IllegalStateException(
                    "[SECURITY] " + field + " must be a UUID v7 (RFC 9562). Found: "
                            + value + " (version " + uuid.version() + ")"
            );
        }

        return uuid;
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[SECURITY] " + field + " cannot be blank");
        }

        return value.trim();
    }
}
