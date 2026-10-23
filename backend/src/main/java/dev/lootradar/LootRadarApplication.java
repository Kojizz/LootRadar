package dev.lootradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LootRadarApplication {

	public static void main(String[] args) {
		SpringApplication.run(LootRadarApplication.class, args);
	}

}
