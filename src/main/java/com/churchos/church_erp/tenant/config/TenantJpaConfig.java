package com.churchos.church_erp.tenant.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Explicit JPA wiring for tenant-scoped entities against {@code tenantRoutingDataSource} (see
 * {@link TenantDataSourceConfig}), which routes every connection acquisition to whichever
 * tenant is set in {@code TenantContext} for the current thread. A repository/entity pair only
 * ever lands in a tenant's own database if it's declared here — never registered globally.
 *
 * <p>As new tenant-scoped business modules (membership, giving, ...) are scaffolded, add their
 * {@code repository}/{@code domain} packages to this one config's {@code basePackages}/
 * {@code @EntityScan} lists rather than creating another {@link EntityManagerFactory}.
 *
 * <p><b>{@code hibernate.dialect}/{@code hibernate.hbm2ddl.auto} are set explicitly below</b> —
 * normally Boot/Hibernate infer both by opening a connection at startup (to inspect JDBC
 * metadata, and to check whether the datasource looks like an embedded test database), but
 * {@code tenantRoutingDataSource} can't provide one at application-startup time:
 * {@code TenantContext} is only ever populated per-request, never at boot, so a routing attempt
 * made now would hit {@code TenantRoutingDataSource}'s "no tenant resolved" guard — which is
 * exactly what was happening here before these were pinned (Boot's embedded-database probe was
 * swallowing that failure and defaulting to {@code create-drop}, i.e. Hibernate dropping and
 * recreating tenant schema on every boot). Pinning both, plus disabling the JDBC-metadata probe
 * via {@code hibernate.temp.use_jdbc_metadata_defaults}, sidesteps all of that — safe because
 * every tenant database is MySQL with Flyway as the only schema authority, per CLAUDE.md.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.churchos.church_erp.security.tenantuser.repository",
    entityManagerFactoryRef = "tenantEntityManagerFactory",
    transactionManagerRef = "tenantTransactionManager"
)
@EntityScan("com.churchos.church_erp.security.tenantuser.domain")
public class TenantJpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("tenantRoutingDataSource") DataSource tenantRoutingDataSource
    ) {
        return builder
            .dataSource(tenantRoutingDataSource)
            .packages("com.churchos.church_erp.security.tenantuser.domain")
            .persistenceUnit("tenant")
            .properties(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.MySQLDialect",
                "hibernate.temp.use_jdbc_metadata_defaults", "false",
                "hibernate.hbm2ddl.auto", "none"
            ))
            .build();
    }

    @Bean
    public PlatformTransactionManager tenantTransactionManager(
        @Qualifier("tenantEntityManagerFactory") EntityManagerFactory tenantEntityManagerFactory
    ) {
        return new JpaTransactionManager(tenantEntityManagerFactory);
    }
}
