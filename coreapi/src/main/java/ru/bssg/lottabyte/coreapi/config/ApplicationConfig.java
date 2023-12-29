package ru.bssg.lottabyte.coreapi.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
public class ApplicationConfig {

    @Value("${bucket}")
    private String bucketName;
    @Value("${external_bucket}")
    private String externalBucketName;
    @Value("${storage_path}")
    private String storagePath;
    @Value("${model_json}")
    private String modelJson;

}