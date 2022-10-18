package com.epam.microservices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class RabbitMQSender {
    private final Logger logger = LoggerFactory.getLogger(RabbitMQSender.class);
    private static final String RESOURCE_ID_UPLOADED_TO_QUEUE = "Resource id {0} uploaded to queue";
    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Retryable
    public void sendUploadedResourceId(Integer id) {
        String resourceIdUploadedToQueue = MessageFormat.format(RESOURCE_ID_UPLOADED_TO_QUEUE, id);
        rabbitTemplate.convertAndSend("uploaded_resourcesIds_queue", String.valueOf(id));
        logger.info(resourceIdUploadedToQueue);
    }
}
