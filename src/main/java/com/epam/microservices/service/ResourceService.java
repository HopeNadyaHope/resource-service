package com.epam.microservices.service;

import com.epam.microservices.model.FileEntity;
import com.epam.microservices.repository.ResourceRepository;
import com.epam.microservices.service.exception.IncorrectRangeException;
import com.epam.microservices.service.exception.ResourceNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.epam.microservices.service.constant.StorageType.PERMANENT;
import static com.epam.microservices.service.constant.StorageType.STAGING;

@Service
public class ResourceService {
    @Autowired
    private ResourceRepository repository;
    @Autowired
    private S3Processor s3Processor;
    @Autowired
    private BucketNameGetter bucketNameGetter;

    public Integer create(MultipartFile file) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setContentType(FilenameUtils.getExtension(file.getOriginalFilename()));
        String stagingBucket = bucketNameGetter.getBucketForStorage(STAGING.getValue());
        fileEntity.setBucket(stagingBucket);

        repository.create(fileEntity);

        int fileEntityId = fileEntity.getId();
        s3Processor.putResource(file, stagingBucket, fileEntityId);

        return fileEntityId;
    }

    public byte[] getFileBytes(Integer id) {
        return repository.read(id)
                .map(fileEntity1 -> s3Processor.getFileBytesFromResource(fileEntity1.getBucket(), id))
                .orElseThrow(() -> new ResourceNotFoundException(id));
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
                s3Processor.deleteResource(fileEntity.get().getBucket(), id);
                repository.delete(fileEntity.get());
                deletedIds.add(id);
            }
        });
        return deletedIds;
    }

    public void permanentResource(Integer id) {
        FileEntity fileEntity = repository.read(id).orElseThrow(() -> new ResourceNotFoundException(id));
        String originBucket = fileEntity.getBucket();
        String destinationBucket = bucketNameGetter.getBucketForStorage(PERMANENT.getValue());
        if (!originBucket.equalsIgnoreCase(destinationBucket)) {
            s3Processor.transferResource(originBucket, destinationBucket, id);
            fileEntity.setBucket(destinationBucket);
            repository.update(fileEntity);
        }
    }

    private boolean isRangeCorrect(int length, int start, int end) {
        return start >= 0 && start <= length - 1 && start != end;
    }
}
