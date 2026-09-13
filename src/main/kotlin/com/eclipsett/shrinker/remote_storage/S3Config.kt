package com.eclipsett.shrinker.remote_storage

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.*;

@Configuration
@EnableScheduling
class S3Config {

    @Bean
    fun s3Client(
        @Value("\${s3.endpoint}") endpoint: String,
        @Value("\${s3.region}") region: String,
        @Value("\${s3.access-key}") accessKey: String,
        @Value("\${s3.secret-key}") secretKey: String
    ): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials
                    .create(accessKey, secretKey))
            )
            .forcePathStyle(true)
            .build();
    }

    @Bean
    fun S3Presigner(
        @Value("\${s3.endpoint}") endpoint: String,
        @Value("\${s3.region}") region: String,
        @Value("\${s3.access-key}") accessKey: String,
        @Value("\${s3.secret-key}") secretKey: String
    ): S3Presigner {
        return S3Presigner.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .build()
    }

}