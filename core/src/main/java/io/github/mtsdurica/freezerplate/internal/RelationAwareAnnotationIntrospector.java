package io.github.mtsdurica.freezerplate.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Applies Spring Data REST-like relation rules through Jackson instead of requiring
 * annotations on user entities: inverse sides are read-only, owning sides are written by id,
 * and nested objects omit the reference back to their parent to avoid cycles.
 */
final class RelationAwareAnnotationIntrospector extends JacksonAnnotationIntrospector {

  @Serial private static final long serialVersionUID = 1L;
  private static final String GETTER_PREFIX = "get";
  private static final String BOOLEAN_GETTER_PREFIX = "is";

  // Only used while serving requests; the mapper holding this introspector is never serialized.
  private final transient JpaEntities entities;

  RelationAwareAnnotationIntrospector(JpaEntities entities) {
    this.entities = entities;
  }

  @Override
  public JsonProperty.Access findPropertyAccess(MapperConfig<?> config, Annotated annotated) {
    if (isInverseSide(annotated)) {
      return JsonProperty.Access.READ_ONLY;
    }
    return super.findPropertyAccess(config, annotated);
  }

  @Override
  public Object findDeserializer(MapperConfig<?> config, Annotated annotated) {
    if (isOwningSide(annotated) && !propertyType(annotated).isContainerType()) {
      return new EntityReferenceDeserializer(entities, propertyType(annotated).getRawClass());
    }
    return super.findDeserializer(config, annotated);
  }

  @Override
  public Object findContentDeserializer(MapperConfig<?> config, Annotated annotated) {
    if (isOwningSide(annotated) && propertyType(annotated).isContainerType()) {
      return new EntityReferenceDeserializer(
          entities, propertyType(annotated).getContentType().getRawClass());
    }
    return super.findContentDeserializer(config, annotated);
  }

  // Missing or null collections become empty so entities whose constructor copies a relation
  // collection do not fail on a request body that omits it.
  @Override
  public JsonSetter.Value findSetterInfo(MapperConfig<?> config, Annotated annotated) {
    if (isRelation(annotated) && propertyType(annotated).isContainerType()) {
      return JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY);
    }
    return super.findSetterInfo(config, annotated);
  }

  @Override
  public JsonIgnoreProperties.Value findPropertyIgnoralByName(
      MapperConfig<?> config, Annotated annotated) {
    return findBackReference(annotated)
        .map(JsonIgnoreProperties.Value::forIgnoredProperties)
        .orElseGet(() -> super.findPropertyIgnoralByName(config, annotated));
  }

  private boolean isOwningSide(Annotated annotated) {
    return isRelation(annotated) && !isInverseSide(annotated);
  }

  private boolean isInverseSide(Annotated annotated) {
    return mappedBy(annotated).isPresent();
  }

  private static JavaType propertyType(Annotated annotated) {
    if (annotated instanceof AnnotatedMethod method && method.getParameterCount() == 1) {
      return method.getParameterType(0);
    }
    return annotated.getType();
  }

  private Optional<String> findBackReference(Annotated annotated) {
    if (!(annotated instanceof AnnotatedMember member) || !isRelation(member)) {
      return Optional.empty();
    }
    return mappedBy(member).or(() -> findInverseSideOf(member));
  }

  private Optional<String> mappedBy(Annotated annotated) {
    return MappedBy.of(annotated::getAnnotation);
  }

  private Optional<String> findInverseSideOf(AnnotatedMember owningSide) {
    Class<?> owner = owningSide.getDeclaringClass();
    String owningProperty = propertyName(owningSide);
    for (Class<?> currentClass = targetClassOf(owningSide.getType());
        currentClass != null;
        currentClass = currentClass.getSuperclass()) {
      for (Field candidate : currentClass.getDeclaredFields()) {
        if (MappedBy.of(candidate::getAnnotation).filter(owningProperty::equals).isPresent()
            && targetClassOf(candidate.getGenericType()).isAssignableFrom(owner)) {
          return Optional.of(candidate.getName());
        }
      }
    }
    return Optional.empty();
  }

  private boolean isRelation(Annotated annotated) {
    return annotated.hasAnnotation(OneToMany.class)
        || annotated.hasAnnotation(ManyToMany.class)
        || annotated.hasAnnotation(OneToOne.class)
        || annotated.hasAnnotation(ManyToOne.class);
  }

  private static String propertyName(AnnotatedMember member) {
    if (!(member instanceof AnnotatedMethod)) {
      return member.getName();
    }
    String stripped = stripGetterPrefix(member.getName());
    return stripped.isEmpty()
        ? stripped
        : Character.toLowerCase(stripped.charAt(0)) + stripped.substring(1);
  }

  private static String stripGetterPrefix(String methodName) {
    if (methodName.startsWith(GETTER_PREFIX)) {
      return methodName.substring(GETTER_PREFIX.length());
    }
    if (methodName.startsWith(BOOLEAN_GETTER_PREFIX)) {
      return methodName.substring(BOOLEAN_GETTER_PREFIX.length());
    }
    return methodName;
  }

  private static Class<?> targetClassOf(JavaType type) {
    return type.isContainerType() ? type.getContentType().getRawClass() : type.getRawClass();
  }

  private static Class<?> targetClassOf(Type type) {
    if (type instanceof ParameterizedType parameterizedType
        && parameterizedType.getRawType() instanceof Class<?> rawClass
        && Collection.class.isAssignableFrom(rawClass)
        && parameterizedType.getActualTypeArguments()[0] instanceof Class<?> elementClass) {
      return elementClass;
    }
    return type instanceof Class<?> typeClass ? typeClass : Object.class;
  }
}
