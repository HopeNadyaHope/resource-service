package com.epam.microservices.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "logstash.enabled", havingValue = "true")
@RefreshScope
public class LogConfig {
    private static final String LOGSTASH_APPENDER_NAME = "LOGSTASH";
    private static final String ASYNC_LOGSTASH_APPENDER_NAME = "ASYNC_LOGSTASH";
    private final Logger logger = LoggerFactory.getLogger(LogConfig.class);
    private final LoggerContext loggerFactory = (LoggerContext) LoggerFactory.getILoggerFactory();
    private final String appName;
    private final String logstashHost;
    private final Integer logstashPort;
    private final Integer logstashQueueSize;

    public LogConfig(
            @Value("${spring.application.name}") String appName,
            @Value("${logstash.host}") String logstashHost,
            @Value("${logstash.port}") Integer logstashPort,
            @Value("${logstash.queue-size}") Integer logstashQueueSize) {
        this.appName = appName;
        this.logstashHost = logstashHost;
        this.logstashPort = logstashPort;
        this.logstashQueueSize = logstashQueueSize;
        addLogstashAppender(loggerFactory);
    }

    private void addLogstashAppender(LoggerContext context) {
        logger.info("Initializing Logstash logging");
        LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
        logstashAppender.setName(LOGSTASH_APPENDER_NAME);
        logstashAppender.setContext(context);
        String customFields = "{\"servicename\":\"" + this.appName + "\"}";
        LogstashEncoder logstashEncoder = new LogstashEncoder();
        logstashEncoder.setCustomFields(customFields);
        logstashAppender.addDestinations(
                new InetSocketAddress(this.logstashHost, this.logstashPort)
        );
        ShortenedThrowableConverter throwableConverter = new ShortenedThrowableConverter();
        throwableConverter.setRootCauseFirst(true);
        logstashEncoder.setThrowableConverter(throwableConverter);
        logstashEncoder.setCustomFields(customFields);
        logstashAppender.setEncoder(logstashEncoder);

        logstashAppender.start();

        AsyncAppender asyncLogstashAppender = new AsyncAppender();
        asyncLogstashAppender.setContext(context);
        asyncLogstashAppender.setName(ASYNC_LOGSTASH_APPENDER_NAME);
        asyncLogstashAppender.setQueueSize(this.logstashQueueSize);
        asyncLogstashAppender.addAppender(logstashAppender);
        asyncLogstashAppender.start();
        context.getLogger("ROOT").addAppender(asyncLogstashAppender);
    }
}
