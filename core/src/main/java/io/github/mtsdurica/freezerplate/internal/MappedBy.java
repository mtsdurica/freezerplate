package io.github.mtsdurica.freezerplate.internal;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The single definition of "inverse side": a relation whose annotation names the owning
 * field in mappedBy. JSON handling and the OpenAPI schemas both rely on it.
 */
final class MappedBy {

  private MappedBy() {}

  static Optional<String> of(AnnotationLookup lookup) {
    return Stream.of(
            Optional.ofNullable(lookup.find(OneToMany.class)).map(OneToMany::mappedBy),
            Optional.ofNullable(lookup.find(ManyToMany.class)).map(ManyToMany::mappedBy),
            Optional.ofNullable(lookup.find(OneToOne.class)).map(OneToOne::mappedBy))
        .flatMap(Optional::stream)
        .filter(mappedBy -> !mappedBy.isEmpty())
        .findFirst();
  }

  @FunctionalInterface
  interface AnnotationLookup {
    <A extends Annotation> A find(Class<A> annotationType);
  }
}
