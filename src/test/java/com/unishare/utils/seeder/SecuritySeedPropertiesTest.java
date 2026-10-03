package com.unishare.utils.seeder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecuritySeedProperties")
class SecuritySeedPropertiesTest {

    private static Map<String, String> validYaml() {
        Map<String, String> yaml = new HashMap<>();
        yaml.put("unishare.security.scanning.enabled", "true");
        yaml.put("unishare.security.scanning.god-account", "true");
        yaml.put("unishare.security.scanning.god-role", "true");
        yaml.put("unishare.security.scanning.roles", "true");
        yaml.put("unishare.security.scanning.permissions", "true");
        yaml.put("unishare.security.seeding.enabled", "true");
        yaml.put("unishare.security.seeding.god-account", "true");
        yaml.put("unishare.security.seeding.god-role", "true");
        yaml.put("unishare.security.seeding.roles", "true");
        yaml.put("unishare.security.seeding.permissions", "true");
        yaml.put("unishare.security.seeding.god-account-role", "true");
        return yaml;
    }

    private static SecuritySeedProperties bind(Map<String, String> yaml) {
        return new Binder(new MapConfigurationPropertySource(yaml))
                .bind("unishare.security", SecuritySeedProperties.class)
                .get();
    }

    private static SecuritySeedProperties with(Consumer<SecuritySeedProperties> change) {
        SecuritySeedProperties properties = bind(validYaml());
        change.accept(properties);
        return properties;
    }

    private static String invalid(Consumer<SecuritySeedProperties> change) {
        return assertThrows(
                SecuritySeedProperties.SecurityConfigurationException.class,
                () -> with(change).validate()
        ).getMessage();
    }

    @Test
    @DisplayName("K. the documented YAML binds and validates")
    void validYamlBinds() {

        SecuritySeedProperties properties = bind(validYaml());

        assertTrue(properties.getScanning().isGodAccount());
        assertTrue(properties.getScanning().isPermissions());
        assertTrue(properties.getSeeding().isGodAccountRole());
        assertDoesNotThrow(properties::validate);
    }

    @Test
    @DisplayName("god-account.initial-password binds from YAML")
    void initialPasswordBinds() {

        Map<String, String> yaml = validYaml();
        yaml.put("unishare.security.god-account.initial-password", "from-env");

        assertEquals("from-env", bind(yaml).getGodAccount().getInitialPassword());
    }

    @Test
    @DisplayName("K. everything disabled is valid")
    void everythingDisabledIsValid() {
        assertDoesNotThrow(() -> with(p -> {
            p.getScanning().setEnabled(false);
            p.getSeeding().setEnabled(false);
        }).validate());
    }

    @Test
    @DisplayName("K. scan-only mode is valid")
    void scanOnlyIsValid() {
        assertDoesNotThrow(() -> with(p -> p.getSeeding().setEnabled(false)).validate());
    }

    @Test
    @DisplayName("L-A. seeding without scanning fails")
    void seedingRequiresScanning() {
        assertTrue(invalid(p -> p.getScanning().setEnabled(false))
                .contains("seeding.enabled=true requires unishare.security.scanning.enabled=true"));
    }

    @Test
    @DisplayName("L-B. god-account seeding without god-account scanning fails")
    void godAccountDependency() {
        assertTrue(invalid(p -> p.getScanning().setGodAccount(false))
                .contains("seeding.god-account=true requires unishare.security.scanning.god-account=true"));
    }

    @Test
    @DisplayName("L-C. roles seeding without roles scanning fails")
    void rolesDependency() {
        assertTrue(invalid(p -> p.getScanning().setRoles(false))
                .contains("seeding.roles=true requires unishare.security.scanning.roles=true"));
    }

    @Test
    @DisplayName("L-D. permissions seeding without permissions scanning fails")
    void permissionsDependency() {
        assertTrue(invalid(p -> p.getScanning().setPermissions(false))
                .contains("seeding.permissions=true requires unishare.security.scanning.permissions=true"));
    }

    @Test
    @DisplayName("L-E. god-account-role seeding without god-account seeding fails")
    void godAccountRoleNeedsGodAccount() {
        assertTrue(invalid(p -> p.getSeeding().setGodAccount(false))
                .contains("seeding.god-account-role=true requires unishare.security.seeding.god-account=true"));
    }

    @Test
    @DisplayName("god-role seeding without god-role scanning fails")
    void godRoleDependency() {
        assertTrue(invalid(p -> {
            p.getScanning().setGodRole(false);
            p.getScanning().setRoles(false);
            p.getSeeding().setRoles(false);
        }).contains("seeding.god-role=true requires unishare.security.scanning.god-role=true"));
    }

    @Test
    @DisplayName("L-F. god-account-role seeding without god-role seeding fails")
    void godAccountRoleNeedsGodRole() {
        assertTrue(invalid(p -> p.getSeeding().setGodRole(false))
                .contains("seeding.god-account-role=true requires unishare.security.seeding.god-role=true"));
    }

    @Test
    @DisplayName("god-account-role does not depend on normal roles seeding")
    void godAccountRoleIndependentOfNormalRoles() {
        assertDoesNotThrow(() -> with(p -> p.getSeeding().setRoles(false)).validate());
    }

    @Test
    @DisplayName("normal roles scanning requires god-role scanning (isolation validation)")
    void rolesScanNeedsGodRoleScan() {
        assertTrue(invalid(p -> {
            p.getScanning().setGodRole(false);
            p.getSeeding().setGodRole(false);
            p.getSeeding().setGodAccountRole(false);
        }).contains("scanning.roles=true requires unishare.security.scanning.god-role=true"));
    }

    @Test
    @DisplayName("master seeding switch off deterministically ignores sub-flags")
    void seedingMasterSwitchIgnoresSubFlags() {
        SecuritySeedProperties properties = with(p -> p.getSeeding().setEnabled(false));
        assertDoesNotThrow(properties::validate);
        assertFalse(properties.getSeeding().shouldSeedGodAccount());
        assertFalse(properties.getSeeding().shouldSeedGodRole());
        assertFalse(properties.getSeeding().shouldSeedGodAccountRole());
    }
}
