package com.unishare.utils.scanner;

import com.unishare.annotation.Permission;
import com.unishare.entity.ride.Ride;
import com.unishare.enums.rbac.PermissionDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PermissionScanner domain discovery")
class PermissionScannerTest {

    static class FixtureController {

        @Permission(id = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d2101", displayName = "App", baseEntity = Ride.class)
        public void application() {
        }

        @Permission(id = "0199c5c4-7b2a-7abc-9e31-4f5a7e8d2102", displayName = "Control", baseEntity = Ride.class,
                domain = PermissionDomain.CONTROL_PLANE)
        public void controlPlane() {
        }

        public void unannotated() {
        }
    }

    @Test
    @DisplayName("D. default domain is APPLICATION; explicit CONTROL_PLANE is read from the annotation")
    void readsDomain() throws Exception {

        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        JpaEntityMetadataResolver resolver = mock(JpaEntityMetadataResolver.class);
        when(resolver.resolveTableName(any())).thenReturn("rides");

        FixtureController controller = new FixtureController();
        Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
        for (String name : new String[]{"application", "controlPlane", "unannotated"}) {
            handlers.put(RequestMappingInfo.paths("/" + name).build(),
                    new HandlerMethod(controller, FixtureController.class.getMethod(name)));
        }
        when(mapping.getHandlerMethods()).thenReturn(handlers);

        Map<UUID, PermissionDefinition> found = new PermissionScanner(mapping, resolver).scan().stream()
                .collect(Collectors.toMap(PermissionDefinition::id, Function.identity()));

        assertEquals(2, found.size());
        assertEquals(PermissionDomain.APPLICATION,
                found.get(UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d2101")).domain());
        assertEquals(PermissionDomain.CONTROL_PLANE,
                found.get(UUID.fromString("0199c5c4-7b2a-7abc-9e31-4f5a7e8d2102")).domain());
    }
}
