package com.epam.microservices.repository;

import com.epam.microservices.entity.FileEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ResourceRepositoryTest {
    @Autowired
    private ResourceRepository repository;

    @Test
    void testCreate() {
        int id = 3;
        String contentType = "mp3";

        FileEntity fileEntity = new FileEntity();
        fileEntity.setContentType(contentType);

        repository.create(fileEntity);

        assertEquals(id, fileEntity.getId());
        assertEquals(contentType, fileEntity.getContentType());
    }

    @Test
    void testRead() {
        int id = 1;
        String contentType = "mp3";

        Optional<FileEntity> fileEntityOptional = repository.read(id);

        assertTrue(fileEntityOptional.isPresent());
        FileEntity fileEntity = fileEntityOptional.get();
        assertEquals(id, fileEntity.getId());
        assertEquals(contentType, fileEntity.getContentType());
    }

    @Test
    void testReadNotFound() {
        int id = 100;

        Optional<FileEntity> fileEntityOptional = repository.read(id);

        assertEquals(Optional.empty(), fileEntityOptional);
    }

}
