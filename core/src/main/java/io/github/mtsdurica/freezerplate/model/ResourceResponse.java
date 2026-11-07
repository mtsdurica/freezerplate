package io.github.mtsdurica.freezerplate.model;

import java.util.Objects;
import java.util.function.Function;

/**
 * Response wrapper for a single resource, serialized as {@code {"data": ...}}.
 *
 * <p>Wrapping leaves room for metadata next to the resource without changing its shape.
 *
 * @param <T> the type of the wrapped resource
 */
public final class ResourceResponse<T> {

  private final T data;

  /**
   * Wraps a resource.
   *
   * @param data the resource
   */
  public ResourceResponse(T data) {
    this.data = data;
  }

  /**
   * Returns the wrapped resource.
   *
   * @return the resource
   */
  public T getData() {
    return data;
  }

  /**
   * Converts the wrapped resource, for example from an entity to its DTO.
   *
   * @param mapper the conversion to apply
   * @param <R> the type of the converted resource
   * @return a new model wrapping the converted resource
   */
  public <R> ResourceResponse<R> map(Function<T, R> mapper) {
    return new ResourceResponse<>(mapper.apply(data));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ResourceResponse<?> resourceResponse && Objects.equals(data, resourceResponse.data);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(data);
  }

  @Override
  public String toString() {
    return "ResourceResponse[data=" + data + "]";
  }
}
