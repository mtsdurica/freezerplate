package io.github.mtsdurica.freezerplate.web;

/**
 * Converts between a request DTO and the entity it creates or changes.
 *
 * <p>Implement this as a Spring bean when a repository declares {@code
 * @FreezerplateApi(request = ...)}. The request DTO defines the body of {@code POST}, {@code PUT}
 * and {@code PATCH}; Bean Validation annotations on it are enforced. Relations are usually
 * referenced by id in the DTO and resolved here; throw {@link UnknownReferenceException} for an id
 * that does not exist to answer with {@code 400 Bad Request}.
 *
 * @param <R> the request DTO type
 * @param <T> the entity type
 */
public interface FreezerplateRequestMapper<R, T> {

  /**
   * Creates a new entity from a validated request; used by {@code POST}.
   *
   * @param request the validated request
   * @return the new, not yet persisted entity
   */
  T toEntity(R request);

  /**
   * Describes the current state of an entity as a request; used by {@code PATCH}.
   *
   * <p>The incoming patch is merged on top of the returned request, so it must contain every field
   * the request exposes. A field left {@code null} here is cleared by any patch that does not
   * mention it.
   *
   * @param entity the stored entity
   * @return a complete request describing the entity
   */
  R toRequest(T entity);

  /**
   * Applies a complete, validated request to an existing entity; used by {@code PUT} and {@code
   * PATCH}.
   *
   * <p>Every field is applied as given, including {@code null} values. The entity is saved
   * afterwards within the same transaction.
   *
   * @param request the validated request
   * @param entity the stored entity to change
   */
  void update(R request, T entity);
}
