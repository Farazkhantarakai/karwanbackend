package com.aiecomm.camp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampApplication.class, args);
	}

}

