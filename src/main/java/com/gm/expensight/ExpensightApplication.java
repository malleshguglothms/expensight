package com.gm.expensight;

import com.gm.expensight.config.TesseractLibraryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExpensightApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ExpensightApplication.class);
		application.addInitializers(new TesseractLibraryInitializer());
		application.run(args);
	}
}
