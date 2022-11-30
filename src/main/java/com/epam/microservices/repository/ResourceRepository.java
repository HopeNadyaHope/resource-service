package com.epam.microservices.repository;

import com.epam.microservices.model.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
public class ResourceRepository {
    private final Logger logger = LoggerFactory.getLogger(ResourceRepository.class);
    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional
    public void create(FileEntity fileEntity) {
        entityManager.persist(fileEntity);
        logger.info("FileEntity with id={} created in database", fileEntity.getId());
    }

    public Optional<FileEntity> read(int id) {
        logger.info("Getting fileEntity with id={} from database", id);
        return Optional.ofNullable(entityManager.find(FileEntity.class, id));
    }

    @Transactional
    public void delete(FileEntity fileEntity) {
        logger.info("Deleting fileEntity with id={} from database", fileEntity.getId());
        entityManager.remove(entityManager.contains(fileEntity) ? fileEntity : entityManager.merge(fileEntity));
    }

    @Transactional
    public void update(FileEntity fileEntity){
        logger.info("Updating fileEntity with id={} from database", fileEntity.getId());
        entityManager.merge(fileEntity);
    }
}
