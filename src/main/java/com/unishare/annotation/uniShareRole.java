package com.unishare.annotation;

import java.lang.annotation.*;

/**
 * Declares a normal application role on the main Spring Boot application class.
 * 
 * This annotation is ONLY scanned from UnishareApplication.class.
 * No classpath-wide scanning is performed.
 * 
 * Multiple roles are declared using @Repeatable pattern.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(uniShareRoles.class)
public @interface uniShareRole {

    /**
     * Immutable UUID v7 identifying this role.
     */
    String id();

    /**
     * Role name (e.g., USER, MODERATOR, ADMIN).
     */
    String name();

    /**
     * Optional description of the role.
     */
    String description() default "";
}