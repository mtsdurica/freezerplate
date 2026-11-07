package io.github.mtsdurica.freezerplate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes an entity field in the response DTO that Freezerplate generates for the entity.
 *
 * <p>Marking at least one field of an entity generates {@code <Entity>Dto}, containing only the
 * marked fields, and {@code <Entity>DtoMapper}, a Spring component, next to the entity. Endpoints
 * of a {@link FreezerplateApi} repository for that entity then respond with the DTO, unless {@link
 * FreezerplateApi#response()} names a different type.
 *
 * <p>A marked relation field becomes a nested DTO, or a list of them. Its target entity must have
 * marked fields as well, otherwise compilation fails. Nested DTOs leave out the field that refers
 * back to the parent, so bidirectional relations do not produce cycles.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface FreezerplatePublic {

  /**
   * The property name in the DTO and in JSON responses.
   *
   * @return the public name, or an empty string to keep the field name
   */
  String name() default "";
}
