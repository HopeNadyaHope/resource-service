package com.epam.microservices.service;

import com.epam.microservices.model.FileEntity;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.epam.microservices.service.constant.StorageType.PERMANENT;
import static com.epam.microservices.service.constant.StorageType.STAGING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class ResourceServiceTest {
    @MockBean
    private ResourceRepository repository;
    @MockBean
    private S3Processor s3Processor;
    @MockBean
    private BucketNameGetter bucketNameGetter;
    @InjectMocks
    private ResourceService service;

    @Test
    void testCreate() {
        MultipartFile file = mock(MultipartFile.class);
        int id = 3;
        String bucket = "staging-bucket";

        when(bucketNameGetter.getBucketForStorage(STAGING.getValue())).thenReturn(bucket);
        doNothing().when(s3Processor).putResource(file, bucket, id);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((FileEntity) args[0]).setId(id);
            return null;
        }).when(repository).create(any(FileEntity.class));

        assertEquals(id, service.create(file));
        verify(repository).create(any(FileEntity.class));
        verifyNoMoreInteractions(repository);
        verify(bucketNameGetter).getBucketForStorage(STAGING.getValue());
        verifyNoMoreInteractions(bucketNameGetter);
        verify(s3Processor).putResource(file, bucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void testGetFileBytes() {
        int id = 3;
        String bucket = "bucket";
        byte[] fileBytes = {0, 1, 2};
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(s3Processor.getFileBytesFromResource(bucket, id)).thenReturn(fileBytes);

        assertArrayEquals(fileBytes, service.getFileBytes(id));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).getFileBytesFromResource(bucket, id);
        verifyNoMoreInteractions(s3Processor);
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
        String bucket = "bucket";
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(s3Processor.getFileBytesFromResource(bucket, id)).thenReturn(fileBytes);

        assertArrayEquals(fileBytesFromRange, service.getFileBytes(id, range));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).getFileBytesFromResource(bucket, id);
        verifyNoMoreInteractions(s3Processor);
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
        String bucket = "bucket";
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(s3Processor.getFileBytesFromResource(bucket, id)).thenReturn(fileBytes);

        assertThrows(IncorrectRangeException.class, () -> service.getFileBytes(id, range));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).getFileBytesFromResource(bucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void testGetFileBytesWithResourceNotFoundException() {
        int id = 3;

        when(repository.read(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getFileBytes(id));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(s3Processor);
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
        verifyNoInteractions(s3Processor);
    }

    @Test
    void testExistedIds() {
        int id = 1;
        String bucket = "bucket";
        List<Integer> ids = List.of(id);
        List<Integer> deletesIds = List.of(id);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(bucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        doNothing().when(s3Processor).deleteResource(bucket, id);
        doNothing().when(repository).delete(fileEntity);

        assertEquals(service.delete(ids), deletesIds);
        verify(repository).read(id);
        verify(repository).delete(fileEntity);
        verifyNoMoreInteractions(repository);
        verify(s3Processor).deleteResource(bucket, id);
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

        service.permanentResource(id);
        verify(repository).read(id);
        verify(repository).update(any(FileEntity.class));
        verifyNoMoreInteractions(repository);
        verify(bucketNameGetter).getBucketForStorage(PERMANENT.getValue());
        verifyNoMoreInteractions(bucketNameGetter);
        verify(s3Processor).transferResource(stagingBucket, permanentBucket, id);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void permanentResourceNotFoundTest() {
        int id = 1;
        when(repository.read(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.permanentResource(id));
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verifyNoMoreInteractions(bucketNameGetter);
        verifyNoMoreInteractions(s3Processor);
    }

    @Test
    void permanentResourceAlreadyPermanentTest() {
        int id = 1;
        String permanentBucket = "permanent-bucket";

        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(id);
        fileEntity.setBucket(permanentBucket);

        when(repository.read(id)).thenReturn(Optional.of(fileEntity));
        when(bucketNameGetter.getBucketForStorage(PERMANENT.getValue())).thenReturn(permanentBucket);

        service.permanentResource(id);
        verify(repository).read(id);
        verifyNoMoreInteractions(repository);
        verify(bucketNameGetter).getBucketForStorage(PERMANENT.getValue());
        verifyNoMoreInteractions(bucketNameGetter);
        verifyNoMoreInteractions(s3Processor);
    }
}
