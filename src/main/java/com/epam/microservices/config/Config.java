package com.epam.microservices.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.epam.microservices.config.dto.Credentials;
import com.epam.microservices.config.dto.EndpointConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
@EnableRetry
public class Config {
    @Bean
    @ConfigurationProperties(prefix = "aws.credentials")
    public Credentials credentials() {
        return new Credentials();
    }

    @Bean
    public AWSCredentials awsCredentials() {
        Credentials credentials = credentials();
        return new BasicAWSCredentials(credentials.getAccessKey(), credentials.getSecretKey());
    }

    @Bean
    @ConfigurationProperties(prefix = "aws.endpoint-config")
    public EndpointConfig endpointConfig() {
        return new EndpointConfig();
    }

    @Bean
    public AmazonS3 s3Client() {
        AwsClientBuilder.EndpointConfiguration config =
                new AwsClientBuilder.EndpointConfiguration(endpointConfig().getEndpoint(), endpointConfig().getRegion());

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials()))
                .withEndpointConfiguration(config)
                .build();
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        return new CommonsMultipartResolver();
    }
}
