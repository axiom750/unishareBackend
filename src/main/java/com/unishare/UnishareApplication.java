package com.unishare;

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

@SpringBootApplication
@EnableScheduling
public class UnishareApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnishareApplication.class, args);
	}

}
