package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.github.mtsdurica.freezerplate.internal.FreezerplateJsonMapper;
import io.github.mtsdurica.freezerplate.model.CollectionResponse;
import io.github.mtsdurica.freezerplate.model.ResourceResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

/**
 * Registered ahead of Spring's default JSON converter but only claims Freezerplate request
 * bodies and response wrappers, so the relation-aware mapper never handles other endpoints.
 * Requests are matched by controller because Spring passes the handler's MethodParameter
 * when reading; responses are matched by type because no handler is available when writing.
 */
final class FreezerplateHttpMessageConverter extends JacksonJsonHttpMessageConverter {

  FreezerplateHttpMessageConverter(FreezerplateJsonMapper jsonMapper) {
    super(jsonMapper.mapper());
  }

  @Override
  public boolean canRead(ResolvableType type, MediaType mediaType) {
    return isFreezerplateRequestBody(type) && super.canRead(type, mediaType);
  }

  @Override
  public boolean canWrite(ResolvableType type, Class<?> valueClass, MediaType mediaType) {
    return isFreezerplateModel(valueClass) && super.canWrite(type, valueClass, mediaType);
  }

  private static boolean isFreezerplateRequestBody(ResolvableType type) {
    return type.getSource() instanceof MethodParameter parameter
        && parameter.getContainingClass().isAnnotationPresent(FreezerplateFor.class);
  }

  private static boolean isFreezerplateModel(Class<?> valueClass) {
    return ResourceResponse.class.isAssignableFrom(valueClass)
        || CollectionResponse.class.isAssignableFrom(valueClass);
  }
}
