package com.example.AUHT_SERVICE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // ← Registrarse en Eureka como microservicio
public class AuhtServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuhtServiceApplication.class, args);
	}

}
