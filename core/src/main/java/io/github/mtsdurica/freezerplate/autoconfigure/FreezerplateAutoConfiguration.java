package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.config.FreezerplateProperties;
import io.github.mtsdurica.freezerplate.internal.FreezerplateJsonMapper;
import io.github.mtsdurica.freezerplate.internal.JpaEntities;
import io.github.mtsdurica.freezerplate.internal.RequestPatcher;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot auto-configuration for Freezerplate, applied automatically when the library is on
 * the classpath.
 *
 * <p>It registers the runtime support used by generated controllers: the optional base path prefix,
 * relation-aware JSON handling, RFC 9457 error responses and the OpenAPI customizations. All of it
 * applies only to generated controllers. Each bean backs off if the application defines its own
 * bean of the same type.
 */
@AutoConfiguration
@EnableConfigurationProperties(FreezerplateProperties.class)
public class FreezerplateAutoConfiguration {

  /** Creates the auto-configuration; instantiated by Spring Boot. */
  public FreezerplateAutoConfiguration() {}

  @Bean
  @ConditionalOnMissingBean
  JpaEntities freezerplateJpaEntities(EntityManagerFactory entityManagerFactory) {
    return new JpaEntities(
        SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
  }

  @Bean
  @ConditionalOnMissingBean
  FreezerplateJsonMapper freezerplateJsonMapper(
      JsonMapper applicationMapper, JpaEntities entities) {
    return new FreezerplateJsonMapper(applicationMapper, entities);
  }

  @Bean
  @ConditionalOnMissingBean
  RequestPatcher freezerplateRequestPatcher(
      FreezerplateJsonMapper jsonMapper, Validator validator) {
    return new RequestPatcher(jsonMapper, validator);
  }

  @Bean
  @ConditionalOnMissingBean
  FreezerplateWebMvcConfigurer freezerplateWebMvcConfigurer(
      FreezerplateProperties properties, FreezerplateJsonMapper jsonMapper) {
    return new FreezerplateWebMvcConfigurer(properties, jsonMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  FreezerplateExceptionHandler freezerplateExceptionHandler() {
    return new FreezerplateExceptionHandler();
  }

  @Bean
  @ConditionalOnMissingBean
  EntityAwareOperationIdCustomizer entityAwareOperationIdCustomizer() {
    return new EntityAwareOperationIdCustomizer();
  }

  @Bean
  @ConditionalOnMissingBean
  ProblemResponsesCustomizer freezerplateProblemResponsesCustomizer() {
    return new ProblemResponsesCustomizer();
  }

  @Bean
  @ConditionalOnMissingBean
  RequestSchemaCustomizer freezerplateRequestSchemaCustomizer(JpaEntities entities) {
    return new RequestSchemaCustomizer(entities);
  }
}
