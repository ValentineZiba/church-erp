package com.churchos.church_erp.tenant.config;

import com.churchos.church_erp.tenant.datasource.TenantDataSourceProvider;
import com.churchos.church_erp.tenant.datasource.TenantRoutingDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Declares the app's two datasources explicitly. Spring Boot's {@code DataSourceAutoConfiguration}
 * backs off entirely as soon as any {@code DataSource} bean exists in the context — so once
 * {@link TenantRoutingDataSource} is registered here, the control-plane datasource must also be
 * declared by hand (built from the same {@code spring.datasource.*} properties as before) and
 * marked {@code @Primary} so JPA/Flyway keep using it, exactly as they did pre-tenant-package.
 */
@Configuration
public class TenantDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties controlPlaneDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource controlPlaneDataSource(DataSourceProperties controlPlaneDataSourceProperties) {
        return controlPlaneDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public DataSource tenantRoutingDataSource(TenantDataSourceProvider tenantDataSourceProvider) {
        return new TenantRoutingDataSource(tenantDataSourceProvider);
    }
}
