package com.epam.microservices.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.microservices.entity.FileEntity;
import com.epam.microservices.repository.ResourceRepository;
import com.epam.microservices.service.exception.IncorrectRangeException;
import com.epam.microservices.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class ResourceServiceTest {
    @MockBean
    private ResourceRepository repository;
    @MockBean
    private AmazonS3 s3;
    @InjectMocks
    private ResourceService service;

    @Test
    void testCreate() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        int id = 3;

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

        assertEquals(id, service.create(file));
        verify(file).getOriginalFilename();
        verify(file).getContentType();
        verify(file).getSize();
        verify(file).getInputStream();
        verifyNoMoreInteractions(file);
        verify(repository).create(any(FileEntity.class));
        verifyNoMoreInteractions(repository);
        verify(s3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void testGetFileBytes() {
        int id = 3;
        byte[] fileBytes = {0, 1, 2};
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(repository.read(id)).thenReturn(Optional.of(mock(FileEntity.class)));
        when(s3.getObject(anyString(), eq(String.valueOf(id)))).thenReturn(s3Object);

        assertArrayEquals(fileBytes, service.getFileBytes(id));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3).getObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

    private static Stream<Arguments> getFileBytesWithRangeTestCases() {
        return Stream.of(
                Arguments.of("FromRangeWithStartAndEnd",
                        List.of(0, 2),
                        new byte[]{0, 1, 2},
                        new byte[]{0, 1}),

                Arguments.of("FromRangeWithStart",
                        List.of(1),
                        new byte[]{0, 1, 2},
                        new byte[]{1, 2})
        );
    }

    @ParameterizedTest(name = "GetFileBytesWithRange_{0}_Test")
    @MethodSource(value = "getFileBytesWithRangeTestCases")
    void testGetFileBytesWithRange(String name,
                                   List<Integer> range,
                                   byte[] fileBytes,
                                   byte[] fileBytesFromRange) {
        int id = 3;
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(repository.read(id)).thenReturn(Optional.of(mock(FileEntity.class)));
        when(s3.getObject(anyString(), eq(String.valueOf(id)))).thenReturn(s3Object);

        assertArrayEquals(fileBytesFromRange, service.getFileBytes(id, range));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3).getObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

    private static Stream<Arguments> getFileBytesWithRangeExceptionTestCases() {
        return Stream.of(
                Arguments.of("FromRangeWithNegativeStart",
                        List.of(-3, 2),
                        new byte[]{0, 1, 2}),

                Arguments.of("FromRangeStartGraterThanBytesLength",
                        List.of(3),
                        new byte[]{0, 1, 2}),

                Arguments.of("FromRangeEqualsStartAndEnd",
                        List.of(2, 2),
                        new byte[]{0, 1, 2})
        );
    }

    @ParameterizedTest(name = "GetFileBytesWithRangeException_{0}_Test")
    @MethodSource(value = "getFileBytesWithRangeExceptionTestCases")
    void testGetFileBytesWithRangeException(String name,
                                            List<Integer> range,
                                            byte[] fileBytes) {
        int id = 3;
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileBytes));

        when(repository.read(id)).thenReturn(Optional.of(mock(FileEntity.class)));
        when(s3.getObject(anyString(), eq(String.valueOf(id)))).thenReturn(s3Object);

        assertThrows(IncorrectRangeException.class, () -> service.getFileBytes(id, range));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3).getObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

    @Test
    void testGetFileBytesWithResourceNotFoundException() {
        int id = 3;

        when(repository.read(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getFileBytes(id));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(s3);
    }

    @Test
    void testDeleteEmptyIds() {
        List<Integer> ids = List.of();
        List<Integer> deletesIds = List.of();

        assertEquals(service.delete(ids), deletesIds);
    }

    @Test
    void testNotExistedIds() {
        int id = 1;
        List<Integer> ids = List.of(id);
        List<Integer> deletesIds = List.of();

        when(repository.read(id)).thenReturn(Optional.empty());

        assertEquals(service.delete(ids), deletesIds);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(s3);
    }

    @Test
    void testExistedIds() {
        int id = 1;
        List<Integer> ids = List.of(id);
        List<Integer> deletesIds = List.of(id);
        FileEntity fileEntity = mock(FileEntity.class);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        doNothing().when(s3).deleteObject(anyString(), eq(String.valueOf(id)));
        doNothing().when(repository).delete(fileEntity);

        assertEquals(service.delete(ids), deletesIds);
        verify(repository).read(id);
        verify(repository).delete(fileEntity);
        verifyNoMoreInteractions(repository);
        verify(s3).deleteObject(anyString(), eq(String.valueOf(id)));
        verifyNoMoreInteractions(s3);
    }

}
