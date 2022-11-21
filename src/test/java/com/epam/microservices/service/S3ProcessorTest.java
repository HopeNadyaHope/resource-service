package com.epam.microservices.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class S3ProcessorTest {
    @MockBean
    private AmazonS3 s3;

    @InjectMocks
    private S3Processor s3Processor;

    @Test
    void putResourceTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        String bucket = "bucket";
        int resourceId = 3;

        when(file.getContentType()).thenReturn("audio/mp3");
        when(file.getSize()).thenReturn(64L);
        when(file.getInputStream()).thenReturn(mock(InputStream.class));
        when(s3.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());

        s3Processor.putResource(file, bucket, resourceId);

        verify(file).getContentType();
        verify(file).getSize();
        verify(file).getInputStream();
        verifyNoMoreInteractions(file);
        verify(s3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void getFileBytesFromResourceTest() {
        int resourceId = 1;
        String bucket = "bucket";
        byte[] fileBytes = {0, 1, 2};
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(s3.getObject(bucket, String.valueOf(resourceId))).thenReturn(s3Object);

        assertArrayEquals(fileBytes, s3Processor.getFileBytesFromResource(bucket, resourceId));
        verify(s3).getObject(bucket, String.valueOf(resourceId));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void deleteResourceTest() {
        String bucket = "bucket";
        int resourceId = 1;

        doNothing().when(s3).deleteObject(bucket, String.valueOf(resourceId));

        s3Processor.deleteResource(bucket, resourceId);

        verify(s3).deleteObject(bucket, String.valueOf(resourceId));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void transferResourceTest() {
        String originBucket = "originBucket";
        String destinationBucket = "destinationBucket";
        int resourceId = 3;
        String key = String.valueOf(resourceId);

        when(s3.copyObject(originBucket, key, destinationBucket, key)).thenReturn(new CopyObjectResult());
        doNothing().when(s3).deleteObject(originBucket, key);

        s3Processor.transferResource(originBucket, destinationBucket, resourceId);

        verify(s3).copyObject(originBucket, key, destinationBucket, key);
        verify(s3).deleteObject(originBucket, key);
        verifyNoMoreInteractions(s3);
    }
}
