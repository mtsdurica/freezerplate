package io.github.mtsdurica.freezerplate.internal;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A private copy of the application's mapper: it keeps the application's Jackson settings but
 * adds Freezerplate's relation handling without changing how the rest of the application
 * reads or writes JSON.
 */
public final class FreezerplateJsonMapper {

  private final JsonMapper mapper;

  public FreezerplateJsonMapper(JsonMapper applicationMapper, JpaEntities entities) {
    this.mapper =
        applicationMapper
            .rebuild()
            .annotationIntrospector(new RelationAwareAnnotationIntrospector(entities))
            .build();
  }

  public JsonMapper mapper() {
    return mapper;
  }

  public <T> T merge(T target, JsonNode patch) {
    return mapper.readerForUpdating(target).readValue(patch);
  }
}
