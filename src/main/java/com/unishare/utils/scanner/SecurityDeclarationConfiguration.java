package com.unishare.utils.scanner;

import com.unishare.utils.seeder.SecuritySeedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the scanned {@link SecurityDeclarations} as a singleton bean.
 *
 * Scanning happens once, during context creation, so invalid configuration
 * or invalid declarations fail startup before the web server accepts traffic.
 * Every consumer (synchronizer, RoleService, ...) receives the same instance.
 */
@Slf4j
@Configuration
public class SecurityDeclarationConfiguration {

    @Bean
    public SecurityDeclarations securityDeclarations(
            SecuritySeedProperties properties,
            SecurityDeclarationScanner scanner
    ) {

        properties.validate();

        if (!properties.getScanning().isEnabled()) {
            log.info("[SECURITY] Declaration scanning disabled (unishare.security.scanning.enabled=false)");
            return SecurityDeclarations.empty();
        }

        return scanner.scan(properties.getScanning());
    }
}
