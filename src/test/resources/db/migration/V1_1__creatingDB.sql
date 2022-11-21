CREATE TABLE IF NOT EXISTS file
(
    `id`               INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `content_type`     VARCHAR(45)        NOT NULL,
    `bucket`           VARCHAR(45)        NOT NULL
);

