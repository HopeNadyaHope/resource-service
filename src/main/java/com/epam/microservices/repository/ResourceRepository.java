package com.epam.microservices.repository;

import com.epam.microservices.entity.FileEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
public class ResourceRepository {
    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional
    public void create(FileEntity fileEntity) {
        entityManager.persist(fileEntity);
    }

    public Optional<FileEntity> read(int id) {
        return Optional.ofNullable(entityManager.find(FileEntity.class, id));
    }

    @Transactional
    public void delete(FileEntity fileEntity) {
        entityManager.remove(entityManager.contains(fileEntity) ? fileEntity : entityManager.merge(fileEntity));
    }
}
