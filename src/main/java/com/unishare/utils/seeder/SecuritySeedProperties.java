package com.unishare.utils.seeder;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution configuration for security scanning and seeding.
 *
 * These flags decide WHETHER scanning/seeding runs. They NEVER describe
 * WHAT is declared: GodAccount, GodRole, role and permission identity and
 * metadata come exclusively from annotations (UnishareApplication for
 * accounts/roles, controller methods for permissions).
 *
 * SCANNING = discover declarations (never writes to the database)
 * SEEDING  = synchronize scanned declarations into the database
 *
 * Seeding depends on scanning; invalid combinations are rejected by
 * {@link #validate()} and block application startup.
 */
@Configuration
@ConfigurationProperties(prefix = "unishare.security")
@Getter
@Setter
public class SecuritySeedProperties {

    private Scanning scanning = new Scanning();

    private Seeding seeding = new Seeding();

    private GodAccountSecret godAccount = new GodAccountSecret();

    /**
     * Secret material for the GodAccount. Identity still comes only from @GodAccount.
     */
    @Getter
    @Setter
    public static class GodAccountSecret {

        /**
         * Initial password, used ONLY when the GodAccount row is first created.
         * Must be supplied via ${GOD_ACCOUNT_INITIAL_PASSWORD}; never a literal in YAML.
         */
        private String initialPassword;
    }


    @Getter
    @Setter
    public static class Scanning {

        /** Master switch. When false, no declarations are scanned. */
        private boolean enabled = true;

        /** Scan @GodAccount from UnishareApplication. */
        private boolean godAccount = true;

        /** Scan @GodRole from UnishareApplication. */
        private boolean godRole = true;

        /** Scan @uniShareRole (normal RBAC roles) from UnishareApplication. */
        private boolean roles = true;

        /** Scan @Permission from controller handler methods. */
        private boolean permissions = true;

        public boolean shouldScanGodAccount() {
            return enabled && godAccount;
        }

        public boolean shouldScanGodRole() {
            return enabled && godRole;
        }

        public boolean shouldScanRoles() {
            return enabled && roles;
        }

        public boolean shouldScanPermissions() {
            return enabled && permissions;
        }
    }

    /**
     * When {@code enabled=false} every sub-flag is ignored and nothing is
     * written (deterministic master switch).
     */
    @Getter
    @Setter
    public static class Seeding {

        /** Master switch. When false, nothing is written to the database. */
        private boolean enabled = true;

        /** Synchronize GodAccount (existing passwords are never overwritten). */
        private boolean godAccount = true;

        /** Synchronize the control-plane GodRole (god_roles). */
        private boolean godRole = true;

        /** Synchronize normal RBAC roles (roles). */
        private boolean roles = true;

        /** Synchronize permissions. */
        private boolean permissions = true;

        /** Synchronize the GodAccount -> GodRole relationship. */
        private boolean godAccountRole = true;

        public boolean shouldSeedGodAccount() {
            return enabled && godAccount;
        }

        public boolean shouldSeedGodRole() {
            return enabled && godRole;
        }

        public boolean shouldSeedRoles() {
            return enabled && roles;
        }

        public boolean shouldSeedPermissions() {
            return enabled && permissions;
        }

        public boolean shouldSeedGodAccountRole() {
            return enabled && godAccountRole;
        }
    }

    /**
     * Validates flag dependencies.
     *
     * Sub-flags only take effect when their master switch is enabled, so
     * dependencies are evaluated on the effective values.
     *
     * @throws SecurityConfigurationException listing every invalid dependency
     */
    public void validate() {

        List<String> errors = new ArrayList<>();

        if (seeding.isEnabled() && !scanning.isEnabled()) {
            errors.add("unishare.security.seeding.enabled=true requires "
                    + "unishare.security.scanning.enabled=true "
                    + "(seeding can only synchronize scanned declarations)");
        }

        if (scanning.isEnabled()) {

            requireScanned(errors, seeding.shouldSeedGodAccount(), scanning.isGodAccount(), "god-account");
            requireScanned(errors, seeding.shouldSeedGodRole(), scanning.isGodRole(), "god-role");
            requireScanned(errors, seeding.shouldSeedRoles(), scanning.isRoles(), "roles");
            requireScanned(errors, seeding.shouldSeedPermissions(), scanning.isPermissions(), "permissions");

            if (scanning.isRoles() && !scanning.isGodRole()) {
                errors.add("unishare.security.scanning.roles=true requires "
                        + "unishare.security.scanning.god-role=true "
                        + "(normal roles are validated against the @GodRole name/id)");
            }
        }

        if (seeding.shouldSeedGodAccountRole()) {

            if (!seeding.isGodAccount()) {
                errors.add("unishare.security.seeding.god-account-role=true requires "
                        + "unishare.security.seeding.god-account=true");
            }

            if (!seeding.isGodRole()) {
                errors.add("unishare.security.seeding.god-account-role=true requires "
                        + "unishare.security.seeding.god-role=true");
            }
        }

        if (!errors.isEmpty()) {
            throw new SecurityConfigurationException(
                    "[SECURITY] Invalid security scanning/seeding configuration: "
                            + String.join("; ", errors)
            );
        }
    }

    private static void requireScanned(List<String> errors, boolean seeded, boolean scanned, String flag) {
        if (seeded && !scanned) {
            errors.add("unishare.security.seeding." + flag + "=true requires "
                    + "unishare.security.scanning." + flag + "=true");
        }
    }

    public static class SecurityConfigurationException extends IllegalStateException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}
