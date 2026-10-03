package com.unishare.utils.scanner;

import com.unishare.annotation.Permission;
import com.unishare.entity.ride.Ride;
import com.unishare.enums.rbac.PermissionDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PermissionScanner: @Permission metadata + canonical name from the SAME
 * method's @PreAuthorize("hasAuthority('NAME')").
 */
@DisplayName("PermissionScanner")
class PermissionScannerTest {

    private static final String RIDE_CREATE_ID = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d21c4";
    private static final String CONTROL_ID = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d2102";

    private RequestMappingHandlerMapping mapping;
    private JpaEntityMetadataResolver resolver;

    @BeforeEach
    void setUp() {
        mapping = mock(RequestMappingHandlerMapping.class);
        resolver = mock(JpaEntityMetadataResolver.class);
        when(resolver.resolveTableName(any())).thenReturn("rides");
    }

    private List<PermissionDefinition> scan(Object controller, String... methods) throws Exception {
        Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
        for (String name : methods) {
            handlers.put(RequestMappingInfo.paths("/" + name).build(),
                    new HandlerMethod(controller, controller.getClass().getMethod(name)));
        }
        when(mapping.getHandlerMethods()).thenReturn(handlers);
        return new PermissionScanner(mapping, resolver).scan();
    }

    private Map<UUID, PermissionDefinition> byId(List<PermissionDefinition> definitions) {
        return definitions.stream().collect(Collectors.toMap(PermissionDefinition::id, Function.identity()));
    }

    private IllegalStateException scanFails(Object controller, String... methods) {
        return assertThrows(IllegalStateException.class, () -> scan(controller, methods));
    }

    // ================================================================ fixtures

    public static class ValidController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", description = "Allows creating a ride",
                baseEntity = Ride.class, domain = PermissionDomain.APPLICATION)
        @PreAuthorize("hasAuthority('RIDE_CREATE')")
        public void createRide() {
        }

        @Permission(id = CONTROL_ID, displayName = "Role Create", description = "Create roles",
                baseEntity = Ride.class, domain = PermissionDomain.CONTROL_PLANE)
        @PreAuthorize("  hasAuthority ( \"ROLE_CREATE\" )  ")
        public void controlPlane() {
        }

        @PreAuthorize("isAuthenticated()")
        public void noPermission() {
        }

        public void unannotated() {
        }
    }

    public static class MissingPreAuthorizeController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        public void createRide() {
        }
    }

    public static class HasRoleController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        @PreAuthorize("hasRole('ADMIN')")
        public void createRide() {
        }
    }

    public static class IsAuthenticatedController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        @PreAuthorize("isAuthenticated()")
        public void createRide() {
        }
    }

    public static class CompoundExpressionController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        @PreAuthorize("hasAuthority('RIDE_CREATE') and isAuthenticated()")
        public void createRide() {
        }
    }

    public static class DuplicateNameController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        @PreAuthorize("hasAuthority('RIDE_CREATE')")
        public void first() {
        }

        @Permission(id = CONTROL_ID, displayName = "Create Ride Again", baseEntity = Ride.class)
        @PreAuthorize("hasAuthority('RIDE_CREATE')")
        public void second() {
        }
    }

    // ================================================================ tests

    @Test
    @DisplayName("1. name is extracted from @PreAuthorize(\"hasAuthority('RIDE_CREATE')\")")
    void extractsName() throws Exception {

        PermissionDefinition definition = byId(scan(new ValidController(), "createRide"))
                .get(UUID.fromString(RIDE_CREATE_ID));

        assertEquals("RIDE_CREATE", definition.name());
    }

    @Test
    @DisplayName("2. complete definition: id, name, displayName, description, domain")
    void completeDefinition() throws Exception {

        Map<UUID, PermissionDefinition> found = byId(scan(new ValidController(),
                "createRide", "controlPlane", "noPermission", "unannotated"));

        assertEquals(2, found.size(), "only @Permission handlers are registered");

        PermissionDefinition ride = found.get(UUID.fromString(RIDE_CREATE_ID));
        assertEquals("RIDE_CREATE", ride.name());
        assertEquals("Create Ride", ride.displayName());
        assertEquals("Allows creating a ride", ride.description());
        assertEquals(PermissionDomain.APPLICATION, ride.domain());
        assertEquals("rides", ride.baseEntity());

        PermissionDefinition control = found.get(UUID.fromString(CONTROL_ID));
        assertEquals("ROLE_CREATE", control.name(), "whitespace and double quotes are tolerated");
        assertEquals("Role Create", control.displayName());
        assertEquals(PermissionDomain.CONTROL_PLANE, control.domain());
    }

    @Test
    @DisplayName("3. @Permission without @PreAuthorize fails, naming controller, method and id")
    void missingPreAuthorizeFails() {

        String message = scanFails(new MissingPreAuthorizeController(), "createRide").getMessage();

        assertTrue(message.contains("MissingPreAuthorizeController#createRide"), message);
        assertTrue(message.contains("permissionId=" + RIDE_CREATE_ID), message);
        assertTrue(message.contains("preAuthorize=<missing>"), message);
    }

    @Test
    @DisplayName("4. hasRole('ADMIN') is not a permission expression and fails")
    void hasRoleFails() {

        String message = scanFails(new HasRoleController(), "createRide").getMessage();

        assertTrue(message.contains("HasRoleController#createRide"), message);
        assertTrue(message.contains("preAuthorize=\"hasRole('ADMIN')\""), message);
        assertTrue(message.contains("unsupported expression"), message);
    }

    @Test
    @DisplayName("5. isAuthenticated() is not a permission expression and fails")
    void isAuthenticatedFails() {

        String message = scanFails(new IsAuthenticatedController(), "createRide").getMessage();

        assertTrue(message.contains("preAuthorize=\"isAuthenticated()\""), message);
        assertTrue(message.contains("unsupported expression"), message);
    }

    @Test
    @DisplayName("compound expressions are not guessed at and fail")
    void compoundExpressionFails() {
        assertTrue(scanFails(new CompoundExpressionController(), "createRide")
                .getMessage().contains("unsupported expression"));
    }

    @Test
    @DisplayName("6. two permission ids resolving to the same name fail")
    void duplicateNameFails() {

        String message = scanFails(new DuplicateNameController(), "first", "second").getMessage();

        assertTrue(message.contains("Duplicate permission name"), message);
        assertTrue(message.contains("name=RIDE_CREATE"), message);
        assertTrue(message.contains("firstId=" + RIDE_CREATE_ID), message);
        assertTrue(message.contains("secondId=" + CONTROL_ID), message);
    }

    @Test
    @DisplayName("real controllers: every @Permission has a valid UUID v7, hasAuthority name, and unique name")
    void realControllersDeclareValidPermissions() {

        Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
        for (Class<?> controller : List.of(
                com.unishare.controller.ride.RideController.class,
                com.unishare.controller.ride.RideRequestController.class,
                com.unishare.controller.user.UpdateUserProfileController.class)) {
            Object bean = mock(controller);
            for (java.lang.reflect.Method method : controller.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                    handlers.put(RequestMappingInfo.paths("/" + controller.getSimpleName() + "/" + method.getName()).build(),
                            new HandlerMethod(bean, method));
                }
            }
        }
        when(mapping.getHandlerMethods()).thenReturn(handlers);

        Map<String, PermissionDefinition> byName = new PermissionScanner(mapping, resolver).scan().stream()
                .collect(Collectors.toMap(PermissionDefinition::name, Function.identity()));

        assertEquals(java.util.Set.of(
                "RIDE_CREATE", "RIDE_UPDATE", "RIDE_DELETE", "RIDE_VIEW_OWN",
                "RIDE_REQUEST_CREATE", "RIDE_REQUEST_APPROVE", "RIDE_REQUEST_DECLINE", "RIDE_REQUEST_CANCEL",
                "RIDE_REQUEST_VIEW_OWN", "RIDE_REQUEST_VIEW", "RIDE_PASSENGER_VIEW",
                "USER_PROFILE_UPDATE"), byName.keySet());

        byName.values().forEach(p -> {
            assertEquals(7, p.id().version(), p.name() + " id must be UUID v7");
            assertEquals(2, p.id().variant(), p.name() + " id must use the RFC 9562 variant");
            assertEquals(PermissionDomain.APPLICATION, p.domain(), p.name());
        });

        assertEquals(UUID.fromString(RIDE_CREATE_ID), byName.get("RIDE_CREATE").id(), "RIDE_CREATE id is frozen");
    }

    @Test
    @DisplayName("D. default domain is APPLICATION")
    void defaultDomain() throws Exception {

        PermissionDefinition definition = scan(new MissingDomainController(), "createRide").get(0);

        assertEquals(PermissionDomain.APPLICATION, definition.domain());
    }

    public static class MissingDomainController {

        @Permission(id = RIDE_CREATE_ID, displayName = "Create Ride", baseEntity = Ride.class)
        @PreAuthorize("hasAuthority('RIDE_CREATE')")
        public void createRide() {
        }
    }
}
