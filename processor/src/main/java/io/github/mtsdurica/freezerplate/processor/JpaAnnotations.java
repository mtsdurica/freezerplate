package io.github.mtsdurica.freezerplate.processor;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Element;

final class JpaAnnotations {

  private JpaAnnotations() {}

  static boolean isCollectionRelation(Element field) {
    return field.getAnnotation(OneToMany.class) != null
        || field.getAnnotation(ManyToMany.class) != null;
  }

  static boolean isOwningCollectionRelation(Element field) {
    return isCollectionRelation(field) && mappedBy(field).isEmpty();
  }

  static Optional<String> mappedBy(Element field) {
    return Stream.of(
            Optional.ofNullable(field.getAnnotation(OneToMany.class)).map(OneToMany::mappedBy),
            Optional.ofNullable(field.getAnnotation(ManyToMany.class)).map(ManyToMany::mappedBy),
            Optional.ofNullable(field.getAnnotation(OneToOne.class)).map(OneToOne::mappedBy))
        .flatMap(Optional::stream)
        .filter(mappedBy -> !mappedBy.isEmpty())
        .findFirst();
  }
}
