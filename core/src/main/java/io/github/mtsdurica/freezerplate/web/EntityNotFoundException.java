package io.github.mtsdurica.freezerplate.web;

import java.io.Serial;

/**
 * Thrown when the entity addressed by a request does not exist.
 *
 * <p>Freezerplate endpoints answer it with {@code 404 Not Found} as an RFC 9457 problem response.
 */
public final class EntityNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates the exception for a missing id.
   *
   * @param id the id that was requested
   */
  public EntityNotFoundException(Object id) {
    super("No entity found for id: " + id);
  }
}
