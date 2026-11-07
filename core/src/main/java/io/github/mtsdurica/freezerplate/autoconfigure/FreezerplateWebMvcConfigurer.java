package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.config.FreezerplateProperties;
import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.github.mtsdurica.freezerplate.internal.FreezerplateJsonMapper;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Applies the configured base path, if any, to generated controllers only and registers the
 * relation-aware JSON converter ahead of Spring's default one.
 */
final class FreezerplateWebMvcConfigurer implements WebMvcConfigurer {

  private final FreezerplateProperties properties;
  private final FreezerplateJsonMapper jsonMapper;

  FreezerplateWebMvcConfigurer(
      FreezerplateProperties properties, FreezerplateJsonMapper jsonMapper) {
    this.properties = properties;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    properties
        .getBasePath()
        .ifPresent(
            basePath ->
                configurer.addPathPrefix(
                    basePath,
                    controllerClass -> controllerClass.isAnnotationPresent(FreezerplateFor.class)));
  }

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    builder.addCustomConverter(new FreezerplateHttpMessageConverter(jsonMapper));
  }
}
