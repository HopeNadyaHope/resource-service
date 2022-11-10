package com.epam.microservices;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.microservices.controller.ResourceController;
import com.epam.microservices.entity.FileEntity;
import com.epam.microservices.repository.ResourceRepository;
import com.epam.microservices.service.RabbitMQSender;
import com.epam.microservices.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private AmazonS3 s3;
    @MockBean
    private RabbitMQSender rabbitMQSender;
    @Autowired
    private ResourceController controller;

    @Test
    void createTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Integer id = 3;
        Map<String, Integer> expectedResult = Map.of(ID, id);

        when(file.getOriginalFilename()).thenReturn("music.mp3");
        when(file.getContentType()).thenReturn("audio/mp3");
        when(file.getSize()).thenReturn(64L);
        when(file.getInputStream()).thenReturn(mock(InputStream.class));
        when(s3.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((FileEntity) args[0]).setId(id);
            return null;
        }).when(repository).create(any(FileEntity.class));
        doNothing().when(rabbitMQSender).sendUploadedResourceId(id);

        assertEquals(expectedResult, controller.create(file));
        verify(service).create(file);
        verifyNoMoreInteractions(service);
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
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(repository.read(id)).thenReturn(Optional.of(mock(FileEntity.class)));
        when(s3.getObject(anyString(), eq(String.valueOf(id)))).thenReturn(s3Object);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.PARTIAL_CONTENT, responseEntity.getStatusCode());
        assertArrayEquals(expectedFileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id, range);
        verify(service).getFileBytes(id);
        verifyNoMoreInteractions(service);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3).getObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void readWithoutRangeTest() {
        int id = 3;
        Map<String, String> headers = Map.of();
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(repository.read(id)).thenReturn(Optional.of(mock(FileEntity.class)));
        when(s3.getObject(anyString(), eq(String.valueOf(id)))).thenReturn(s3Object);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertArrayEquals(fileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id);
        verifyNoMoreInteractions(service);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3).getObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void deleteTest() {
        List<Integer> ids = List.of(1, 2, 3);
        List<Integer> deletedIds = List.of(1, 3);
        Map<String, List<Integer>> expectedResult = Map.of(ID, deletedIds);
        FileEntity fileEntity = new FileEntity();

        when(repository.read(1)).thenReturn(Optional.of(fileEntity));
        when(repository.read(2)).thenReturn(Optional.empty());
        when(repository.read(3)).thenReturn(Optional.of(fileEntity));
        doNothing().when(repository).delete(any(FileEntity.class));
        doNothing().when(s3).deleteObject(anyString(), anyString());

        assertEquals(expectedResult, controller.delete(ids));
        verify(service).delete(ids);
        verifyNoMoreInteractions(service);
        verify(repository, times(3)).read(anyInt());
        verify(repository, times(2)).delete(any());
        verifyNoMoreInteractions(repository);
        verify(s3, times(2)).deleteObject(anyString(), anyString());
        verifyNoMoreInteractions(s3);
    }

}
