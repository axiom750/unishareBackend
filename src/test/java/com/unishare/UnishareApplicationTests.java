package com.unishare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs against the dev profile (application-local.yaml). Seeding is disabled so
 * the test never writes to the dev database; declaration scanning and config
 * validation still run during context startup.
 */
@SpringBootTest(properties = "unishare.security.seeding.enabled=false")
@ActiveProfiles("local")
class UnishareApplicationTests {

	@Test
	void contextLoads() {
	}

}
