package io.github.mtsdurica.freezerplate.web;

import java.io.Serial;
import java.util.Collection;

/**
 * Thrown when a request refers to a related entity that does not exist.
 *
 * <p>Freezerplate endpoints answer it with {@code 400 Bad Request} as an RFC 9457 problem
 * response; when raised while reading a request body, the response points at the offending field.
 * Request mappers should throw it for unknown ids they resolve themselves.
 */
public final class UnknownReferenceException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates the exception for several unknown ids of the same relation.
   *
   * @param ids the ids that could not be found
   */
  public UnknownReferenceException(Collection<?> ids) {
    super("No entities found for ids: " + ids);
  }

  /**
   * Creates the exception for one unknown id of a given entity type.
   *
   * @param entityType the type of the referenced entity
   * @param id the id that could not be found
   */
  public UnknownReferenceException(Class<?> entityType, Object id) {
    super("No " + entityType.getSimpleName() + " with id " + id);
  }
}
