package com.epam.microservices.service;

import com.epam.microservices.client.ApiGatewayClient;
import com.epam.microservices.model.StorageModel;
import com.epam.microservices.service.exception.NoBucketForStorageTypeException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.epam.microservices.service.constant.StorageType.PERMANENT;

@Component
public class BucketNameGetter {
    private static final String PERMANENT_BUCKET = "permanent-resources";
    private static final String STAGING_BUCKET = "staging-resources";
    private final Logger logger = LoggerFactory.getLogger(BucketNameGetter.class);

    @Autowired
    private ApiGatewayClient apiGatewayClient;

    @CircuitBreaker(name = "storageServiceCallCB", fallbackMethod = "getDefaultBucket")
    public String getBucketForStorage(String storageType) {
        logger.info("Getting storages for storage type{}", storageType);
        List<StorageModel> storages = apiGatewayClient.getStorages();
        String availableStorageTypes = storages.stream()
                .map(StorageModel::getStorageType)
                .collect(Collectors.joining(", "));
        logger.info("Got available storages for storage types: {}", availableStorageTypes);
        return storages.stream()
                .filter(storage -> storageType.equalsIgnoreCase(storage.getStorageType()))
                .map(StorageModel::getBucket)
                .findFirst()
                .orElseThrow(() -> new NoBucketForStorageTypeException(storageType));
    }

    public String getDefaultBucket(String storageType, Exception e) {
        logger.error("Get default bucket name for storage service call error: {}", e.getMessage());
        return PERMANENT.getValue().equalsIgnoreCase(storageType) ? PERMANENT_BUCKET : STAGING_BUCKET;
    }
}
