package com.unishare.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple @uniShareRole declarations.
 * This is the @Repeatable container for @uniShareRole.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface uniShareRoles {
    
    /**
     * Array of uniShareRole annotation declarations.
     */
    com.unishare.annotation.uniShareRole[] value();
}
