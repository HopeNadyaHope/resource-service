package com.epam.microservices;

import com.epam.microservices.controller.ResourceController;
import com.epam.microservices.model.FileEntity;
import com.epam.microservices.repository.ResourceRepository;
import com.epam.microservices.service.BucketNameGetter;
import com.epam.microservices.service.RabbitMQSender;
import com.epam.microservices.service.ResourceService;
import com.epam.microservices.service.S3Processor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.epam.microservices.service.constant.StorageType.PERMANENT;
import static com.epam.microservices.service.constant.StorageType.STAGING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class IntegrationTest {
    private static final String ID = "id";
    private static final String RANGE = "range";
    @SpyBean
    private ResourceService service;
    @MockBean
    private ResourceRepository repository;
    @MockBean
    private S3Processor s3Processor;
    @MockBean
    private BucketNameGetter bucketNameGetter;
    @MockBean
    private RabbitMQSender rabbitMQSender;
    @Autowired
    private ResourceController controller;

    @Test
    void createTest() {
        MultipartFile file = mock(MultipartFile.class);
        Integer id = 3;
        Map<String, Integer> expectedResult = Map.of(ID, id);
        String bucket = "staging-bucket";

        when(bucketNameGetter.getBucketForStorage(STAGING.getValue())).thenReturn(bucket);
        doNothing().when(s3Processor).putResource(file, bucket, id);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((FileEntity) args[0]).setId(id);
            return null;
        }).when(repository).create(any(FileEntity.class));
        doNothing().when(rabbitMQSender).sendUploadedResourceId(id);

        assertEquals(expectedResult, controller.create(file));
        verify(service).create(file);
        verifyNoMoreInteractions(service);
        verify(bucketNameGetter).getBucketForStorage(STAGING.getValue());
        verifyNoMoreInteractions(bucketNameGetter);
        verify(s3Processor).putResource(file, bucket, id);
        verifyNoMoreInteractions(s3Processor);
        verify(repository).create(any(FileEntity.class));
        verifyNoMoreInteractions(repository);
        verify(rabbitMQSender).sendUploadedResourceId(id);
        verifyNoMoreInteractions(rabbitMQSender);
    }

    @Test
    void readWithRangeTest() {
        int id = 3;
        Map<String, String> headers = Map.of(RANGE, "0 - 3");
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
        byte[] expectedFileBytes = new byte[]{1, 2, 3};
        List<Integer> range = List.of(0, 3);
        String bucket = "staging-bucket";
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(s3Processor.getFileBytesFromResource(bucket, id)).thenReturn(fileBytes);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.PARTIAL_CONTENT, responseEntity.getStatusCode());
        assertArrayEquals(expectedFileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id, range);
        verify(service).getFileBytes(id);
        verifyNoMoreInteractions(service);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).getFileBytesFromResource(bucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void readWithoutRangeTest() {
        int id = 3;
        Map<String, String> headers = Map.of();
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
        String bucket = "staging-bucket";
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(s3Processor.getFileBytesFromResource(bucket, id)).thenReturn(fileBytes);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertArrayEquals(fileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id);
        verifyNoMoreInteractions(service);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).getFileBytesFromResource(bucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void deleteTest() {
        List<Integer> ids = List.of(1, 2, 3);
        List<Integer> deletedIds = List.of(1, 3);
        Map<String, List<Integer>> expectedResult = Map.of(ID, deletedIds);
        String bucket = "bucket";
        FileEntity fileEntity = new FileEntity();
        fileEntity.setBucket(bucket);

        when(repository.read(1)).thenReturn(Optional.of(fileEntity));
        when(repository.read(2)).thenReturn(Optional.empty());
        when(repository.read(3)).thenReturn(Optional.of(fileEntity));
        doNothing().when(repository).delete(any(FileEntity.class));
        doNothing().when(s3Processor).deleteResource(eq(bucket), any(Integer.class));

        assertEquals(expectedResult, controller.delete(ids));
        verify(service).delete(ids);
        verifyNoMoreInteractions(service);
        verify(repository, times(3)).read(anyInt());
        verify(repository, times(2)).delete(any());
        verifyNoMoreInteractions(repository);
        verify(s3Processor, times(2)).deleteResource(eq(bucket), any(Integer.class));
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void permanentResourceTest() {
        int id = 1;
        String stagingBucket = "staging-bucket";
        String permanentBucket = "permanent-bucket";

        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(stagingBucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(bucketNameGetter.getBucketForStorage(PERMANENT.getValue())).thenReturn(permanentBucket);
        doNothing().when(s3Processor).transferResource(stagingBucket, permanentBucket, id);
        doNothing().when(repository).update(any(FileEntity.class));

        controller.permanentResource(id);
        verify(service).permanentResource(id);
        verifyNoMoreInteractions(service);
        verify(repository).read(id);
        verify(repository).update(any(FileEntity.class));
        verifyNoMoreInteractions(repository);
        verify(bucketNameGetter).getBucketForStorage(PERMANENT.getValue());
        verifyNoMoreInteractions(bucketNameGetter);
        verify(s3Processor).transferResource(stagingBucket, permanentBucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

}
