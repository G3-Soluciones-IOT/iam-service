package pe.edu.upc.iam_service.shared.infrastructure.persistence.jpa.configuration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMigrationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayMigrationConfiguration.class);
    private static final String FLYWAY_MIGRATOR_BEAN_NAME = "iamFlywayMigrator";

    @Bean
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource, Environment environment) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations(csvProperty(environment, "spring.flyway.locations", "classpath:db/migration"))
                .validateOnMigrate(booleanProperty(environment, "spring.flyway.validate-on-migrate", true))
                .outOfOrder(booleanProperty(environment, "spring.flyway.out-of-order", false))
                .baselineOnMigrate(booleanProperty(environment, "spring.flyway.baseline-on-migrate", true))
                .baselineVersion(environment.getProperty("spring.flyway.baseline-version", "1"));

        return configuration.load();
    }

    @Bean(name = FLYWAY_MIGRATOR_BEAN_NAME)
    public InitializingBean iamFlywayMigrator(Flyway flyway) {
        return () -> {
            LOGGER.info("Running Flyway migrations before Hibernate validation");
            flyway.migrate();
        };
    }

    @Bean
    public static BeanFactoryPostProcessor iamEntityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            addDependsOn(beanFactory, "entityManagerFactory", FLYWAY_MIGRATOR_BEAN_NAME);

            String[] entityManagerFactoryBeanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    beanFactory,
                    jakarta.persistence.EntityManagerFactory.class,
                    true,
                    false);

            for (String beanName : entityManagerFactoryBeanNames) {
                addDependsOn(beanFactory, beanName, FLYWAY_MIGRATOR_BEAN_NAME);
            }
        };
    }

    private static void addDependsOn(
            ConfigurableListableBeanFactory beanFactory,
            String beanName,
            String dependencyBeanName) {

        if (!beanFactory.containsBeanDefinition(beanName)) {
            return;
        }

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        LinkedHashSet<String> dependsOn = new LinkedHashSet<>();
        String[] currentDependsOn = beanDefinition.getDependsOn();

        if (currentDependsOn != null) {
            dependsOn.addAll(Arrays.asList(currentDependsOn));
        }

        dependsOn.add(dependencyBeanName);
        beanDefinition.setDependsOn(dependsOn.toArray(String[]::new));
    }

    private static String[] csvProperty(Environment environment, String propertyName, String defaultValue) {
        return Arrays.stream(environment.getProperty(propertyName, defaultValue).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    private static boolean booleanProperty(Environment environment, String propertyName, boolean defaultValue) {
        return environment.getProperty(propertyName, Boolean.class, defaultValue);
    }
}
