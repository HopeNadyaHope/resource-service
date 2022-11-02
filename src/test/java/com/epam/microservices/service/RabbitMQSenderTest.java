package com.epam.microservices.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class RabbitMQSenderTest {
    @MockBean
    private AmqpTemplate rabbitTemplate;
    @InjectMocks
    private RabbitMQSender rabbitMQSender;

    @Test
    void sendUploadedResourceIdTest() {
        int id = 1;

        doNothing().when(rabbitTemplate).convertAndSend(anyString(), eq(String.valueOf(id)));

        assertDoesNotThrow(() -> rabbitMQSender.sendUploadedResourceId(id));
        verify(rabbitTemplate).convertAndSend(anyString(), eq(String.valueOf(id)));
    }
}
