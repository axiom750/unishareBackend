package com.unishare.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GodAccount {

    /**
     * Immutable UUID v7 identifying the God Account.
     */
    String id();

    /**
     * Reserved username of the God Account.
     */
    String username();

    /**
     * Reserved email of the God Account.
     */
    String email();

    /**
     * Display name of the God Account.
     */
    String displayName() default "UniShare God Account";
}