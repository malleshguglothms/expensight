package com.gm.expensight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "storage.location=test-upload-dir")
class ExpensightApplicationTests {

	@Test
	void contextLoads() {
	}

}
