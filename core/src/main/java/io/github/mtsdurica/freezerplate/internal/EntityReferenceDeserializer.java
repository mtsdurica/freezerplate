package io.github.mtsdurica.freezerplate.internal;

import io.github.mtsdurica.freezerplate.web.UnknownReferenceException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Throws UnknownReferenceException rather than a Jackson error so the exception handler can
 * recognise it inside Jackson's wrapping and point the problem response at the exact field.
 */
final class EntityReferenceDeserializer extends ValueDeserializer<Object> {

  private final JpaEntities entities;
  private final Class<?> entityClass;

  EntityReferenceDeserializer(JpaEntities entities, Class<?> entityClass) {
    this.entities = entities;
    this.entityClass = entityClass;
  }

  @Override
  public Object deserialize(JsonParser parser, DeserializationContext context) {
    Object id = context.readValue(parser, entities.idTypeOf(entityClass));
    return entities
        .find(entityClass, id)
        .orElseThrow(() -> new UnknownReferenceException(entityClass, id));
  }
}
