package com.app.suspiro;

import com.app.suspiro.config.DotenvLoader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuspiroApplication {
	
	public static void main(String[] args) {
		DotenvLoader.init();
		SpringApplication.run(SuspiroApplication.class, args);
	}

}
