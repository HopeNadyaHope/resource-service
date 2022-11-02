package com.epam.microservices.controller;

import com.epam.microservices.service.RabbitMQSender;
import com.epam.microservices.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class ResourceControllerTest {
    private static final String ID = "id";
    private static final String RANGE = "range";
    @MockBean
    private ResourceService service;
    @MockBean
    private RabbitMQSender rabbitMQSender;
    @InjectMocks
    private ResourceController controller;

    @Test
    void createTest() {
        MultipartFile file = mock(MultipartFile.class);
        Integer id = 3;
        Map<String, Integer> expectedResult = Map.of(ID, id);

        when(service.create(file)).thenReturn(id);
        doNothing().when(rabbitMQSender).sendUploadedResourceId(id);

        assertEquals(expectedResult, controller.create(file));
        verify(service).create(file);
        verifyNoMoreInteractions(service);
        verify(rabbitMQSender).sendUploadedResourceId(id);
        verifyNoMoreInteractions(rabbitMQSender);
    }

    @Test
    void readWithRangeTest() {
        int id = 3;
        Map<String, String> headers = Map.of(RANGE, "0 - 5");
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
        List<Integer> range = List.of(0, 5);

        when(service.getFileBytes(id, range)).thenReturn(fileBytes);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.PARTIAL_CONTENT, responseEntity.getStatusCode());
        assertArrayEquals(fileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id, range);
        verifyNoMoreInteractions(service);
    }

    @Test
    void readWithoutRangeTest() {
        int id = 3;
        Map<String, String> headers = Map.of();
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};

        when(service.getFileBytes(id)).thenReturn(fileBytes);

        ResponseEntity<byte[]> responseEntity = controller.read(id, headers);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertArrayEquals(fileBytes, responseEntity.getBody());
        verify(service).getFileBytes(id);
        verifyNoMoreInteractions(service);
    }

    @Test
    void deleteTest() {
        List<Integer> ids = List.of(1, 2, 3);
        List<Integer> deletedIds = List.of(1, 3);
        Map<String, List<Integer>> expectedResult = Map.of(ID, deletedIds);

        when(service.delete(ids)).thenReturn(deletedIds);

        assertEquals(expectedResult, controller.delete(ids));
        verify(service).delete(ids);
        verifyNoMoreInteractions(service);
    }

}
