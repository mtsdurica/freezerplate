package io.github.mtsdurica.freezerplate.model;

import java.util.List;
import java.util.function.Function;

/**
 * Response wrapper for a list of resources, serialized as {@code {"items": [...]}}.
 *
 * <p>Wrapping leaves room for metadata such as pagination next to the items without changing
 * their shape.
 *
 * @param <T> the type of the listed resources
 */
public final class CollectionResponse<T> {

  private final List<T> items;

  /**
   * Wraps a list of resources; the list is copied.
   *
   * @param items the resources, without {@code null} elements
   */
  public CollectionResponse(List<T> items) {
    this.items = List.copyOf(items);
  }

  /**
   * Returns the listed resources.
   *
   * @return an unmodifiable list of the resources
   */
  public List<T> getItems() {
    return items;
  }

  /**
   * Converts every listed resource, for example from entities to their DTOs.
   *
   * @param mapper the conversion to apply to each resource
   * @param <R> the type of the converted resources
   * @return a new model listing the converted resources
   */
  public <R> CollectionResponse<R> map(Function<T, R> mapper) {
    return new CollectionResponse<>(items.stream().map(mapper).toList());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof CollectionResponse<?> collectionResponse && items.equals(collectionResponse.items);
  }

  @Override
  public int hashCode() {
    return items.hashCode();
  }

  @Override
  public String toString() {
    return "CollectionResponse[items=" + items + "]";
  }
}
