package io.github.mtsdurica.freezerplate.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

final class RepositoryCollector {

  private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
  private static final String REPOSITORY_PACKAGE_SUFFIX = ".repository";
  private static final String CONTROLLER_PACKAGE_SUFFIX = ".controller";
  private static final String DEFAULT_PLURAL_SUFFIX = "s";
  private static final Pattern SURROUNDING_SLASHES = Pattern.compile("^/+|/+$");
  private static final Pattern INVALID_PATH_CHARACTERS = Pattern.compile("[\\s{}]");

  private final ProcessingEnvironment processingEnvironment;
  private final SourceWriter sourceWriter;

  RepositoryCollector(ProcessingEnvironment processingEnvironment, SourceWriter sourceWriter) {
    this.processingEnvironment = processingEnvironment;
    this.sourceWriter = sourceWriter;
  }

  Map<String, RepositoryInfo> collect(
      RoundEnvironment roundEnvironment, Map<String, ClassName> dtoTypesByEntityName) {
    var repositoriesByEntityName = new LinkedHashMap<String, RepositoryInfo>();
    for (Element element : roundEnvironment.getElementsAnnotatedWith(FreezerplateApi.class)) {
      if (element.getKind() != ElementKind.INTERFACE) {
        sourceWriter.error("@FreezerplateApi only applies to repository interfaces", element);
        continue;
      }
      TypeElement repositoryElement = (TypeElement) element;
      Optional<List<? extends TypeMirror>> typeArguments =
          jpaRepositoryTypeArguments(repositoryElement);
      if (typeArguments.isEmpty()) {
        sourceWriter.error(
            "Could not resolve JpaRepository<T, ID> - must directly extend JpaRepository",
            repositoryElement);
        continue;
      }
      TypeMirror entity = typeArguments.get().get(0);
      TypeMirror id = typeArguments.get().get(1);
      repositoriesByEntityName.put(
          entity.toString(),
          toRepositoryInfo(repositoryElement, entity, id, dtoTypesByEntityName));
    }
    return repositoriesByEntityName;
  }

  private RepositoryInfo toRepositoryInfo(
      TypeElement repositoryElement,
      TypeMirror entity,
      TypeMirror id,
      Map<String, ClassName> dtoTypesByEntityName) {
    FreezerplateApi annotation = repositoryElement.getAnnotation(FreezerplateApi.class);
    TypeName entityType = TypeName.get(entity);
    String entityName = simpleName(entityType);
    Optional<TypeName> explicitResponseType = declaredDtoType(annotation::response);
    Optional<TypeName> responseDtoType =
        explicitResponseType.or(
            () -> Optional.ofNullable(dtoTypesByEntityName.get(entity.toString())));

    return RepositoryInfo.builder()
        .repositoryElement(repositoryElement)
        .repositoryType(ClassName.get(repositoryElement))
        .entityType(entityType)
        .idType(TypeName.get(id))
        .entityName(entityName)
        .path(pathFor(annotation, entityName, repositoryElement))
        .controllerPackage(controllerPackageFor(repositoryElement))
        .responseType(responseDtoType.orElse(entityType), responseDtoType.isPresent())
        .requestType(declaredDtoType(annotation::request))
        .build();
  }

  private String pathFor(FreezerplateApi annotation, String entityName, TypeElement repository) {
    String configured = annotation.path().strip();
    if (configured.isEmpty()) {
      return "/" + entityName.toLowerCase() + DEFAULT_PLURAL_SUFFIX;
    }
    String trimmed = SURROUNDING_SLASHES.matcher(configured).replaceAll("");
    if (trimmed.isEmpty() || INVALID_PATH_CHARACTERS.matcher(trimmed).find()) {
      sourceWriter.error(
          "@FreezerplateApi path \"" + annotation.path() + "\" must be a path such as "
              + "\"categories\" or \"library/books\", without whitespace or {placeholders}",
          repository);
    }
    return "/" + trimmed;
  }

  // Reading a Class-valued annotation attribute during annotation processing always throws,
  // because the class may not be compiled yet; the exception carries the type mirror instead.
  private static Optional<TypeName> declaredDtoType(Runnable classAttributeAccess) {
    try {
      classAttributeAccess.run();
    } catch (MirroredTypeException mirroredTypeException) {
      TypeMirror type = mirroredTypeException.getTypeMirror();
      return type.getKind() == TypeKind.VOID ? Optional.empty() : Optional.of(TypeName.get(type));
    }
    throw new IllegalStateException("Class attribute did not throw MirroredTypeException");
  }

  private static Optional<List<? extends TypeMirror>> jpaRepositoryTypeArguments(
      TypeElement repositoryElement) {
    return repositoryElement.getInterfaces().stream()
        .filter(DeclaredType.class::isInstance)
        .map(DeclaredType.class::cast)
        .filter(supertype -> TypeMirrors.qualifiedName(supertype).equals(JPA_REPOSITORY))
        .<List<? extends TypeMirror>>map(DeclaredType::getTypeArguments)
        .findFirst();
  }

  private String controllerPackageFor(TypeElement repositoryElement) {
    String repositoryPackage =
        processingEnvironment
            .getElementUtils()
            .getPackageOf(repositoryElement)
            .getQualifiedName()
            .toString();
    if (!repositoryPackage.endsWith(REPOSITORY_PACKAGE_SUFFIX)) {
      return repositoryPackage;
    }
    return repositoryPackage.substring(
            0, repositoryPackage.length() - REPOSITORY_PACKAGE_SUFFIX.length())
        + CONTROLLER_PACKAGE_SUFFIX;
  }

  private static String simpleName(TypeName typeName) {
    String qualifiedName = typeName.toString();
    return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
  }
}
