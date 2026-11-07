package io.github.mtsdurica.freezerplate.processor;

import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

final class TypeMirrors {

  private static final Set<String> COLLECTION_TYPES =
      Set.of("java.util.Set", "java.util.List", "java.util.Collection");

  private TypeMirrors() {}

  static Optional<TypeMirror> unwrapCollectionElement(TypeMirror type) {
    if (!(type instanceof DeclaredType declaredType)
        || !COLLECTION_TYPES.contains(qualifiedName(declaredType))
        || declaredType.getTypeArguments().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(declaredType.getTypeArguments().get(0));
  }

  static boolean isCollection(TypeMirror type) {
    return unwrapCollectionElement(type).isPresent();
  }

  static TypeMirror elementOrSelf(TypeMirror type) {
    return unwrapCollectionElement(type).orElse(type);
  }

  static String qualifiedName(TypeMirror type) {
    return type instanceof DeclaredType declaredType
        ? ((TypeElement) declaredType.asElement()).getQualifiedName().toString()
        : type.toString();
  }

  static String capitalize(String value) {
    return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
