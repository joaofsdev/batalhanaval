package com.softexpert.batalhanaval_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatalhanavalApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatalhanavalApiApplication.class, args);
	}

}
