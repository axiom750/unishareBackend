package com.unishare.utils.seeder;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for security seeding and permission scanning.
 * 
 * Controls:
 * - Whether role seeding is enabled
 * - SUPERUSER bootstrap email
 * - Permission scanner enablement
 */
@Configuration
@ConfigurationProperties(prefix = "unishare.security")
@Getter
@Setter
public class SecuritySeedProperties {

    /**
     * Seed configuration for roles and SUPERUSER bootstrap.
     */
    private Seed seed = new Seed();

    /**
     * Permission scanner configuration.
     */
    private Permission permission = new Permission();

    @Getter
    @Setter
    public static class Seed {
        /**
         * Whether to run default role and SUPERUSER seeding on startup.
         * Default: true
         */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Permission {
        /**
         * Scanner configuration.
         */
        private Scanner scanner = new Scanner();
    }

    @Getter
    @Setter
    public static class Scanner {
        /**
         * Whether to run permission scanner on startup.
         * Default: true
         */
        private boolean enabled = true;
    }

    /**
     * SUPERUSER configuration.
     */
    private Superuser superuser = new Superuser();

    @Getter
    @Setter
    public static class Superuser {
        /**
         * Whether to run SUPERUSER bootstrap on startup.
         * Set to false when you need to create the SUPERUSER account first.
         * Default: false (must explicitly enable)
         */
        private boolean enabled = false;
        
        /**
         * Email of the user who will receive SUPERUSER role on first bootstrap.
         * REQUIRED for SUPERUSER bootstrap to execute.
         * Leave empty to skip SUPERUSER bootstrap.
         */
        private String email;
    }
}
