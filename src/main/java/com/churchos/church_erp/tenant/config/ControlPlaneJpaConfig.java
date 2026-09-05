package com.churchos.church_erp.tenant.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Explicit JPA wiring for control-plane entities ({@code platform.domain}, {@code tenant.domain})
 * against {@code controlPlaneDataSource}. Needed as soon as a second, tenant-scoped
 * {@link EntityManagerFactory} exists (see {@link TenantJpaConfig}) — Spring Boot's implicit
 * single-EMF autoconfiguration can no longer guess which datasource an entity belongs to, so both
 * halves must be declared by hand with non-overlapping {@code basePackages}/{@code @EntityScan}.
 * Marked {@code @Primary} so anything that doesn't specify an entity-manager-factory qualifier
 * (e.g. {@code @Transactional} without a value) keeps defaulting here, matching pre-tenant-JPA
 * behavior.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.churchos.church_erp.platform.repository",
        "com.churchos.church_erp.tenant.repository"
    },
    entityManagerFactoryRef = "controlPlaneEntityManagerFactory",
    transactionManagerRef = "controlPlaneTransactionManager"
)
@EntityScan({
    "com.churchos.church_erp.platform.domain",
    "com.churchos.church_erp.tenant.domain"
})
public class ControlPlaneJpaConfig {

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean controlPlaneEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("controlPlaneDataSource") DataSource controlPlaneDataSource
    ) {
        return builder
            .dataSource(controlPlaneDataSource)
            .packages("com.churchos.church_erp.platform.domain", "com.churchos.church_erp.tenant.domain")
            .persistenceUnit("controlPlane")
            .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager controlPlaneTransactionManager(
        @Qualifier("controlPlaneEntityManagerFactory") EntityManagerFactory controlPlaneEntityManagerFactory
    ) {
        return new JpaTransactionManager(controlPlaneEntityManagerFactory);
    }
}
