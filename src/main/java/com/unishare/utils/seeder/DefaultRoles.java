package com.unishare.utils.seeder;

import java.util.UUID;

public final class DefaultRoles {

    private DefaultRoles() {
    }

    // Frozen UUID v7 role IDs — DO NOT CHANGE

    public static final UUID USER_ID =
            UUID.fromString("01a0fe4b-f2ea-73a5-ad8d-866b14ba8160");

    public static final UUID MODERATOR_ID =
            UUID.fromString("01a0fe4b-f2ea-7efb-8b5b-826023276850");

    public static final UUID ADMIN_ID =
            UUID.fromString("01a0fe4b-f2ea-7aea-94fe-7a296412f964");

    public static final UUID SUPERUSER_ID =
            UUID.fromString("01a0fe4b-f2ea-72bb-b39f-fe77132c0be8");

    public static final String USER = "USER";
    public static final String MODERATOR = "MODERATOR";
    public static final String ADMIN = "ADMIN";
    public static final String SUPERUSER = "SUPERUSER";
}