package tech.ipim.sweng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SwengApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwengApplication.class, args);
		System.out.println("\n=== NOTA BENE Backend Started ===");
		System.out.println("Server: http://localhost:8080/api");
		System.out.println("Health: http://localhost:8080/api/auth/health");
		System.out.println("H2 Console: http://localhost:8080/h2-console");
		System.out.println("==================================\n");
	}
}