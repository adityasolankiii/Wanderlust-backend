package com.wanderlust.wanderlust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WanderlustApplication {

	public static void main(String[] args) {
		SpringApplication.run(WanderlustApplication.class, args);
	}

}
