package ru.bssg.lottabyte.usermanagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Lottabyte Data Governance User Management API", version = "0.1", description = "API to search governance data"))
@SpringBootApplication(scanBasePackages={"ru.bssg.lottabyte"})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
