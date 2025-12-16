package com.gm.expensight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "storage.location=test-upload-dir",
        "llm.openrouter.api-key=test-key",
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class ExpensightApplicationTests {

	@Test
	void contextLoads() {
	}

}
