package com.epam.microservices.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "file")
public class FileEntity {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name="content_type")
    private String contentType;

    private String bucket;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntity that = (FileEntity) o;
        return id == that.id && contentType.equals(that.contentType) && bucket.equals(that.bucket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contentType, bucket);
    }
}
