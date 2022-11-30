package com.epam.microservices.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.microservices.service.exception.ResourceCantBeReachedException;
import com.epam.microservices.service.exception.UnableToSaveFileException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@Component
public class S3Processor {
    private final Logger logger = LoggerFactory.getLogger(S3Processor.class);
    @Autowired
    private AmazonS3 s3;

    public void putResource(MultipartFile file, String bucket, Integer resourceId) {
        try {
            s3.putObject(bucket, String.valueOf(resourceId),
                    file.getInputStream(), getUploadObjectMetadata(file));
            logger.info("Resource with id={} uploaded to s3_bucket={}", resourceId, bucket);
        } catch (Exception e) {
            throw new UnableToSaveFileException(e);
        }
    }

    public byte[] getFileBytesFromResource(String bucket, Integer resourceId) {
        S3Object s3object = s3.getObject(bucket, String.valueOf(resourceId));
        logger.info("Resource with id={} downloaded from s3_bucket={}", resourceId, bucket);
        try {
            return IOUtils.toByteArray(s3object.getObjectContent());
        } catch (IOException e) {
            throw new ResourceCantBeReachedException(resourceId);
        }
    }

    public void deleteResource(String bucket, Integer resourceId) {
        s3.deleteObject(bucket, String.valueOf(resourceId));
        logger.info("Resource with id={} deleted from s3_bucket={}", resourceId, bucket);
    }

    public void transferResource(String originBucket, String destinationBucket, Integer resourceId) {
        String key = String.valueOf(resourceId);
        s3.copyObject(originBucket, key, destinationBucket, key);
        logger.info("Resource with id={} copied to {}", resourceId, destinationBucket);
        s3.deleteObject(originBucket, key);
        logger.info("Resource with id={} deleted from s3_bucket={}", resourceId, originBucket);
    }

    private ObjectMetadata getUploadObjectMetadata(MultipartFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setLastModified(Date.from(Instant.now()));
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());
        return objectMetadata;
    }

}
