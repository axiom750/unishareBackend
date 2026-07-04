package com.unishare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnishareApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnishareApplication.class, args);
	}

}
