package io.github.mtsdurica.freezerplate.internal;

import io.github.mtsdurica.freezerplate.model.CollectionResponse;
import io.github.mtsdurica.freezerplate.model.ResourceResponse;
import io.github.mtsdurica.freezerplate.web.EntityNotFoundException;
import java.util.function.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import tools.jackson.databind.JsonNode;

/**
 * The CRUD operations behind every generated entity controller. Generated controllers stay thin
 * and delegate here, so the behaviour lives in tested library code instead of generated code.
 */
public final class FreezerplateCrud<T, ID, R extends JpaRepository<T, ID>> {

  private final R repository;
  private final FreezerplateJsonMapper jsonMapper;
  private final JpaEntities entities;

  public FreezerplateCrud(R repository, FreezerplateJsonMapper jsonMapper, JpaEntities entities) {
    this.repository = repository;
    this.jsonMapper = jsonMapper;
    this.entities = entities;
  }

  public CollectionResponse<T> findAll() {
    return new CollectionResponse<>(repository.findAll());
  }

  public ResourceResponse<T> findOne(ID id) {
    return new ResourceResponse<>(load(id));
  }

  public ResourceResponse<T> create(T entity) {
    return new ResourceResponse<>(repository.save(entity));
  }

  // Follows Spring Data REST: PUT replaces plain fields but keeps the stored relations, which
  // are changed through PATCH or the association endpoints instead.
  public ResourceResponse<T> update(ID id, T entity) {
    entities.copyAssociations(load(id), entity);
    entities.setId(entity, id);
    return new ResourceResponse<>(repository.save(entity));
  }

  public ResourceResponse<T> patch(ID id, JsonNode patch) {
    return new ResourceResponse<>(repository.save(jsonMapper.merge(load(id), patch)));
  }

  public ResourceResponse<T> modify(ID id, Consumer<T> modification) {
    T entity = load(id);
    modification.accept(entity);
    return new ResourceResponse<>(repository.save(entity));
  }

  public void delete(ID id) {
    if (!repository.existsById(id)) {
      throw new EntityNotFoundException(id);
    }
    repository.deleteById(id);
  }

  private T load(ID id) {
    return repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }
}
