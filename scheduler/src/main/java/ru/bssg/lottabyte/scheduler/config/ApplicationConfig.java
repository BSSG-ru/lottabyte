package ru.bssg.lottabyte.scheduler.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ApplicationConfig {
    @Value("${lottabyte.api.url}")
    private String lottabyteApiUrl;

    @Value("${lottabyte.api.username}")
    private String lottabyteApiUsername;

    @Value("${lottabyte.api.password}")
    private String lottabyteApiPassword;

    @Value("${lottabyte.api.token}")
    private String lottabyteApiToken;

    @Value("${lottabyte.api.language}")
    private String lottabyteApiLanguage;
}
