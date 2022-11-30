package com.epam.microservices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQSender {
    private final Logger logger = LoggerFactory.getLogger(RabbitMQSender.class);
    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Retryable
    public void sendUploadedResourceId(Integer id) {
        rabbitTemplate.convertAndSend("uploaded_resourcesIds_queue", String.valueOf(id));
        logger.info("Resource with id={} uploaded to queue", id);
    }
}
