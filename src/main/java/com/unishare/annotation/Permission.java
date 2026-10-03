package com.unishare.annotation;

import com.unishare.enums.rbac.PermissionDomain;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {

    /**
     * Immutable UUID v7 identifying this permission.
     */
    String id();

    /**
     * Human-readable permission name.
     */
    String displayName();

    /**
     * Description of the permission.
     */
    String description() default "";

    /**
     * Primary JPA entity controlled by this permission.
     */
    Class<?> baseEntity();

    /**
     * Additional JPA entities reachable by this permission.
     */
    Class<?>[] reachableEntities() default {};

    /**
     * Authorization domain. APPLICATION permissions are granted to normal roles;
     * CONTROL_PLANE permissions are granted automatically to the declared GodRole.
     */
    PermissionDomain domain() default PermissionDomain.APPLICATION;
}