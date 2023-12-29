package ru.bssg.lottabyte.scheduler;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.bssg.lottabyte.core.usermanagement.config.JwtConfiguration;

@OpenAPIDefinition(info = @Info(title = "Lottabyte Scheduler", version = "0.1", description = "Lottabyte Schdeuler"))
@SpringBootApplication(
        scanBasePackages={"ru.bssg.lottabyte"}
)
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
