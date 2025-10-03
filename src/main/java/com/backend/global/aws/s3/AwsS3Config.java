package com.backend.global.aws.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("prod")
public class AwsS3Config {
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.AP_NORTHEAST_2)
            // credentials 지정 안 함 → EC2 IAM Role 자동 사용
            .build();
    }
}