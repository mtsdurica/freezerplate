package io.github.mtsdurica.freezerplate.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Reads entity structure from the JPA metamodel instead of scanning annotations itself, so ids
 * and relations are interpreted exactly as Hibernate interprets them.
 */
public final class JpaEntities {

  private final EntityManager entityManager;

  public JpaEntities(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public Class<?> idTypeOf(Class<?> entityClass) {
    return entityType(entityClass).getIdType().getJavaType();
  }

  public <T> Optional<T> find(Class<T> entityClass, Object id) {
    return Optional.ofNullable(entityManager.find(entityClass, id));
  }

  public Object idOf(Object entity) {
    return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
  }

  public String idNameOf(Class<?> entityClass) {
    return idAttributeOf(entityClass).getName();
  }

  public List<Relation> relationsOf(Class<?> entityClass) {
    return entityType(entityClass).getAttributes().stream()
        .filter(Attribute::isAssociation)
        .map(this::toRelation)
        .toList();
  }

  public void setId(Object entity, Object id) {
    write(entity, idAttributeOf(entity.getClass()), id);
  }

  public void copyAssociations(Object source, Object target) {
    for (Attribute<?, ?> attribute : entityType(source.getClass()).getAttributes()) {
      if (attribute.isAssociation()) {
        write(target, attribute, read(source, attribute));
      }
    }
  }

  private EntityType<?> entityType(Class<?> entityClass) {
    return entityManager.getMetamodel().entity(entityClass);
  }

  private SingularAttribute<?, ?> idAttributeOf(Class<?> entityClass) {
    return entityType(entityClass).getSingularAttributes().stream()
        .filter(SingularAttribute::isId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No id on " + entityClass));
  }

  private Relation toRelation(Attribute<?, ?> attribute) {
    Class<?> target =
        attribute instanceof PluralAttribute<?, ?, ?> pluralAttribute
            ? pluralAttribute.getElementType().getJavaType()
            : attribute.getJavaType();
    boolean owning = MappedBy.of(fieldOf(attribute)::getAnnotation).isEmpty();
    return new Relation(attribute.getName(), owning, attribute.isCollection(), idTypeOf(target));
  }

  private static Object read(Object entity, Attribute<?, ?> attribute) {
    try {
      return fieldOf(attribute).get(entity);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException("Could not read " + attribute.getName(), exception);
    }
  }

  private static void write(Object entity, Attribute<?, ?> attribute, Object value) {
    try {
      fieldOf(attribute).set(entity, value);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException("Could not write " + attribute.getName(), exception);
    }
  }

  // Reflection on fields keeps entities free of Freezerplate-specific setters; property-access
  // entities would need getter/setter handling, which is not supported yet.
  private static Field fieldOf(Attribute<?, ?> attribute) {
    if (!(attribute.getJavaMember() instanceof Field field)) {
      throw new IllegalStateException(
          "Freezerplate supports field access only, but "
              + attribute.getName()
              + " uses property access");
    }
    field.setAccessible(true);
    return field;
  }

  public static final class Relation {

    private final String name;
    private final boolean owning;
    private final boolean collection;
    private final Class<?> targetIdType;

    Relation(String name, boolean owning, boolean collection, Class<?> targetIdType) {
      this.name = name;
      this.owning = owning;
      this.collection = collection;
      this.targetIdType = targetIdType;
    }

    public String getName() {
      return name;
    }

    public boolean isOwning() {
      return owning;
    }

    public boolean isCollection() {
      return collection;
    }

    public Class<?> getTargetIdType() {
      return targetIdType;
    }
  }
}
