package io.github.mtsdurica.freezerplate.internal;

import io.github.mtsdurica.freezerplate.model.CollectionResponse;
import io.github.mtsdurica.freezerplate.web.UnknownReferenceException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The list, replace, add and remove operations behind generated association controllers such as
 * {@code /books/{id}/authors}, working on the owning side's collection by target ids.
 */
public final class FreezerplateAssociation<A, AID, AR extends JpaRepository<A, AID>> {

  private final AR associatedRepository;
  private final JpaEntities entities;

  public FreezerplateAssociation(AR associatedRepository, JpaEntities entities) {
    this.associatedRepository = associatedRepository;
    this.entities = entities;
  }

  public CollectionResponse<A> findAll(Supplier<Set<A>> getter) {
    return new CollectionResponse<>(List.copyOf(getter.get()));
  }

  public void replace(Consumer<Set<A>> setter, List<AID> ids) {
    setter.accept(loadAll(ids));
  }

  public void add(Supplier<Set<A>> getter, Consumer<Set<A>> setter, List<AID> ids) {
    Set<A> current = new HashSet<>(getter.get());
    current.addAll(loadAll(ids));
    setter.accept(current);
  }

  public void remove(Supplier<Set<A>> getter, Consumer<Set<A>> setter, AID id) {
    Set<A> current =
        getter.get().stream()
            .filter(associated -> !Objects.equals(entities.idOf(associated), id))
            .collect(Collectors.toCollection(HashSet::new));
    setter.accept(current);
  }

  private Set<A> loadAll(List<AID> ids) {
    List<A> found = associatedRepository.findAllById(ids);
    Set<Object> foundIds = found.stream().map(entities::idOf).collect(Collectors.toSet());
    List<AID> unknownIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
    if (!unknownIds.isEmpty()) {
      throw new UnknownReferenceException(unknownIds);
    }
    return new HashSet<>(found);
  }
}
