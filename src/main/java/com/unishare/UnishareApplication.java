package com.unishare;

import com.unishare.annotation.GodAccount;
import com.unishare.annotation.GodRole;
import com.unishare.annotation.uniShareRole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *  
 *  UniShare – A university a community platform for university students to connect, share, and collaborate. 
 *  Handles Users, Assets, and Scores with a PostgreSQL database via Supabase platform built with Spring Boot.
 *
 *
 *  @author  Pravin Choudhary
 *  @since  1.0
 *
 *
 */

@GodAccount(
		id = "01a0fe4b-f2ea-7d8c-9a3f-1e5c4b7a9d2f",
		username = "superuser",
		email = "superuser.unishare@gmail.com",
		displayName = "UniShare God Account"
)
@GodRole(
		id = "01a0fe4b-f2ea-72bb-b39f-fe77132c0be8",
		name = "SUPERUSER",
		description = "Root control-plane role for UniShare."
)
@uniShareRole(
		id = "01a0fe4b-f2ea-73a5-ad8d-866b14ba8160",
		name = "USER",
		description = "Default role for normal UniShare users."
)
@uniShareRole(
		id = "01a0fe4b-f2ea-7efb-8b5b-826023276850",
		name = "MODERATOR",
		description = "Moderation role for UniShare."
)
@uniShareRole(
		id = "01a0fe4b-f2ea-7aea-94fe-7a296412f964",
		name = "ADMIN",
		description = "Administrative role for UniShare."
)
@SpringBootApplication
@EnableScheduling
public class UnishareApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnishareApplication.class, args);
	}

}
