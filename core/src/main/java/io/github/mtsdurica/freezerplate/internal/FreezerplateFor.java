package io.github.mtsdurica.freezerplate.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks generated controllers at runtime; path prefixing, JSON handling, error handling and the
 * OpenAPI customizers all scope themselves to classes carrying it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FreezerplateFor {

  Class<?> value();

  String association() default "";
}
