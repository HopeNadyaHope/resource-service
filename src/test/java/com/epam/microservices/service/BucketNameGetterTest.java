package com.epam.microservices.service;

import com.epam.microservices.client.ApiGatewayClient;
import com.epam.microservices.model.StorageModel;
import com.epam.microservices.service.exception.NoBucketForStorageTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.epam.microservices.service.constant.StorageType.PERMANENT;
import static com.epam.microservices.service.constant.StorageType.STAGING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class BucketNameGetterTest {
    @MockBean
    private ApiGatewayClient apiGatewayClient;

    @InjectMocks
    private BucketNameGetter bucketNameGetter;

    @Test
    void getBucketForStorageTest() {
        String stagingBucket = "staging-resources";
        String permanentBucket = "permanent-resources";

        StorageModel stagingStorage = new StorageModel();
        stagingStorage.setId(1);
        stagingStorage.setStorageType(STAGING.getValue());
        stagingStorage.setBucket(stagingBucket);

        StorageModel permanentStorage = new StorageModel();
        permanentStorage.setId(1);
        permanentStorage.setStorageType(PERMANENT.getValue());
        permanentStorage.setBucket(permanentBucket);

        when(apiGatewayClient.getStorages()).thenReturn(List.of(stagingStorage, permanentStorage));
        assertEquals(stagingBucket, bucketNameGetter.getBucketForStorage(STAGING.getValue()));
        verify(apiGatewayClient).getStorages();
        verifyNoMoreInteractions(apiGatewayClient);
    }

    @Test
    void getBucketForStorageExceptionTest() {
        String stagingBucket = "staging-resources";

        StorageModel stagingStorage = new StorageModel();
        stagingStorage.setId(1);
        stagingStorage.setStorageType(STAGING.getValue());
        stagingStorage.setBucket(stagingBucket);

        when(apiGatewayClient.getStorages()).thenReturn(List.of(stagingStorage));
        assertThrows(NoBucketForStorageTypeException.class,
                () -> bucketNameGetter.getBucketForStorage(PERMANENT.getValue()));
        verify(apiGatewayClient).getStorages();
        verifyNoMoreInteractions(apiGatewayClient);
    }

    @Test
    void getDefaultBucketForPermanentTest() {
        assertEquals("permanent-resources",
                bucketNameGetter.getDefaultBucket(PERMANENT.getValue(), new RuntimeException()));
    }

    @Test
    void getDefaultBucketForStagingTest() {
        assertEquals("staging-resources",
                bucketNameGetter.getDefaultBucket(STAGING.getValue(), new RuntimeException()));
    }
}
