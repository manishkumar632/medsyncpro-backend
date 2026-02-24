package com.medsyncpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedsyncproApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedsyncproApplication.class, args);
	}

}
