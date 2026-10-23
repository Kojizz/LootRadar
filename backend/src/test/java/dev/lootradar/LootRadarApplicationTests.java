package dev.lootradar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"lootradar.sync-enabled=false", "spring.datasource.url=jdbc:h2:mem:context;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
class LootRadarApplicationTests {

	@Test
	void contextLoads() {
	}

}
