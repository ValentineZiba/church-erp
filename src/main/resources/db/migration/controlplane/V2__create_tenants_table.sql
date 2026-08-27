CREATE TABLE tenants (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(63)  NOT NULL,
    subdomain     VARCHAR(255) NOT NULL,
    db_host       VARCHAR(255) NOT NULL,
    db_port       INT          NOT NULL DEFAULT 3306,
    db_name       VARCHAR(64)  NOT NULL,
    db_username   VARCHAR(255) NOT NULL,
    db_password   VARCHAR(255) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_tenants_slug UNIQUE (slug),
    CONSTRAINT uk_tenants_subdomain UNIQUE (subdomain),
    CONSTRAINT uk_tenants_db_name UNIQUE (db_name)
) ENGINE = InnoDB;
