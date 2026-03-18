package com.aimentor.common.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:0}") String baselineVersion
    ) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion);
        return new Flyway(configuration);
    }

    @Bean("flywayMigrator")
    public InitializingBean flywayMigrator(Flyway flyway) {
        return flyway::migrate;
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] currentDependsOn = entityManagerFactory.getDependsOn();
            if (currentDependsOn == null || currentDependsOn.length == 0) {
                entityManagerFactory.setDependsOn("flywayMigrator");
                return;
            }

            String[] nextDependsOn = new String[currentDependsOn.length + 1];
            System.arraycopy(currentDependsOn, 0, nextDependsOn, 0, currentDependsOn.length);
            nextDependsOn[currentDependsOn.length] = "flywayMigrator";
            entityManagerFactory.setDependsOn(nextDependsOn);
        };
    }
}
