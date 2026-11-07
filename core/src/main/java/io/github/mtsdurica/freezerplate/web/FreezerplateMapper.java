package io.github.mtsdurica.freezerplate.web;

/**
 * Converts an entity into the DTO that Freezerplate endpoints return for it.
 *
 * <p>Implement this as a Spring bean when a repository declares {@code
 * @FreezerplateApi(response = ...)}; mappers for DTOs generated from {@code @FreezerplatePublic} are
 * generated automatically. The mapper is applied to every response for the entity, including
 * association endpoints that list it.
 *
 * @param <T> the entity type
 * @param <D> the DTO type
 */
public interface FreezerplateMapper<T, D> {

  /**
   * Converts an entity into its DTO.
   *
   * <p>Runs after the entity was loaded, while the request's persistence context is still open, so
   * lazy relations can be read. Mapping related entities that map back to this one is the
   * implementation's responsibility; following both directions recurses endlessly.
   *
   * @param entity the entity to convert, never {@code null}
   * @return the DTO to serialize
   */
  D toDto(T entity);
}
