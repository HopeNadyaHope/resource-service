package com.epam.microservices.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.microservices.entity.FileEntity;
import com.epam.microservices.repository.ResourceRepository;
import com.epam.microservices.service.exception.IncorrectRangeException;
import com.epam.microservices.service.exception.ResourceCantBeReachedException;
import com.epam.microservices.service.exception.UnableToSaveFileException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class ResourceService {

    private static final String BUCKET_NAME = "resources";

    @Autowired
    private ResourceRepository repository;

    @Autowired
    private AmazonS3 s3;

    public Integer create(MultipartFile file) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setContentType(FilenameUtils.getExtension(file.getOriginalFilename()));

        try {
            repository.create(fileEntity);
            s3.putObject(BUCKET_NAME, String.valueOf(fileEntity.getId()),
                    file.getInputStream(), getUploadObjectMetadata(file));
        } catch (Exception e) {
            throw new UnableToSaveFileException();
        }

        return fileEntity.getId();
    }

    public byte[] getFileBytes(Integer id) {
        return repository.read(id)
                .map(fileEntity1 -> getFileBytesFromResource(id))
                .orElseThrow(() -> new ObjectNotFoundException(id, FileEntity.class.getName()));
    }

    public byte[] getFileBytes(Integer id, List<Integer> range) {
        byte[] bytes = getFileBytes(id);
        int start = range.get(0);
        int end = range.size() > 1 ? range.get(1) : bytes.length;

        if (!isRangeCorrect(bytes.length, start, end)) {
            throw new IncorrectRangeException(start, end);
        }

        return Arrays.copyOfRange(bytes, start, end);
    }

    public List<Integer> delete(List<Integer> ids) {
        List<Integer> deletedIds = new ArrayList<>();
        ids.forEach(id -> {
            Optional<FileEntity> fileEntity = repository.read(id);
            if (fileEntity.isPresent()) {
                s3.deleteObject(BUCKET_NAME, String.valueOf(id));
                repository.delete(fileEntity.get());
                deletedIds.add(id);
            }
        });
        return deletedIds;
    }

    private ObjectMetadata getUploadObjectMetadata(MultipartFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setLastModified(Date.from(Instant.now()));
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());
        return objectMetadata;
    }

    private byte[] getFileBytesFromResource(int id) {
        S3Object s3object = s3.getObject(BUCKET_NAME, String.valueOf(id));
        try {
            return IOUtils.toByteArray(s3object.getObjectContent());
        } catch (IOException e) {
            throw new ResourceCantBeReachedException(id);
        }
    }

    private boolean isRangeCorrect(int length, int start, int end) {
        return start >= 0 && start <= length - 1 && start != end;
    }
}
