package io.github.mtsdurica.freezerplate.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.TypeElement;

final class RepositoryInfo {

  private final TypeElement repositoryElement;
  private final ClassName repositoryType;
  private final TypeName entityType;
  private final TypeName idType;
  private final String entityName;
  private final String path;
  private final String controllerPackage;
  private final TypeName responseType;
  private final boolean hasResponseDto;
  private final Optional<TypeName> requestType;

  private RepositoryInfo(Builder builder) {
    this.repositoryElement = Objects.requireNonNull(builder.repositoryElement);
    this.repositoryType = Objects.requireNonNull(builder.repositoryType);
    this.entityType = Objects.requireNonNull(builder.entityType);
    this.idType = Objects.requireNonNull(builder.idType);
    this.entityName = Objects.requireNonNull(builder.entityName);
    this.path = Objects.requireNonNull(builder.path);
    this.controllerPackage = Objects.requireNonNull(builder.controllerPackage);
    this.responseType = Objects.requireNonNull(builder.responseType);
    this.hasResponseDto = builder.hasResponseDto;
    this.requestType = Objects.requireNonNull(builder.requestType);
  }

  static Builder builder() {
    return new Builder();
  }

  TypeElement getRepositoryElement() {
    return repositoryElement;
  }

  ClassName getRepositoryType() {
    return repositoryType;
  }

  TypeName getEntityType() {
    return entityType;
  }

  TypeName getIdType() {
    return idType;
  }

  String getEntityName() {
    return entityName;
  }

  String getPath() {
    return path;
  }

  String getControllerPackage() {
    return controllerPackage;
  }

  TypeName getResponseType() {
    return responseType;
  }

  boolean hasResponseDto() {
    return hasResponseDto;
  }

  Optional<TypeName> getRequestType() {
    return requestType;
  }

  boolean hasRequestDto() {
    return requestType.isPresent();
  }

  static final class Builder {

    private TypeElement repositoryElement;
    private ClassName repositoryType;
    private TypeName entityType;
    private TypeName idType;
    private String entityName;
    private String path;
    private String controllerPackage;
    private TypeName responseType;
    private boolean hasResponseDto;
    private Optional<TypeName> requestType = Optional.empty();

    private Builder() {}

    Builder repositoryElement(TypeElement repositoryElement) {
      this.repositoryElement = repositoryElement;
      return this;
    }

    Builder repositoryType(ClassName repositoryType) {
      this.repositoryType = repositoryType;
      return this;
    }

    Builder entityType(TypeName entityType) {
      this.entityType = entityType;
      return this;
    }

    Builder idType(TypeName idType) {
      this.idType = idType;
      return this;
    }

    Builder entityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    Builder path(String path) {
      this.path = path;
      return this;
    }

    Builder controllerPackage(String controllerPackage) {
      this.controllerPackage = controllerPackage;
      return this;
    }

    Builder responseType(TypeName responseType, boolean hasResponseDto) {
      this.responseType = responseType;
      this.hasResponseDto = hasResponseDto;
      return this;
    }

    Builder requestType(Optional<TypeName> requestType) {
      this.requestType = requestType;
      return this;
    }

    RepositoryInfo build() {
      return new RepositoryInfo(this);
    }
  }
}
