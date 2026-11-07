package io.github.mtsdurica.freezerplate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a REST controller with CRUD endpoints for the annotated Spring Data JPA repository.
 *
 * <p>The repository must directly extend {@code JpaRepository<Entity, Id>}. At compile time the
 * Freezerplate annotation processor writes {@code <Entity>FreezerplateController}, serving
 * {@code GET}, {@code POST}, {@code PUT}, {@code PATCH} and {@code DELETE} under {@link #path()},
 * for example {@code /books}. If {@code freezerplate.base-path} is set, it is prepended, for
 * example {@code /api/books}. For every owning-side collection relation whose target also has a
 * {@code @FreezerplateApi} repository, an association controller such as {@code
 * /books/{id}/authors} is generated as well.
 *
 * <p>If the repository package ends in {@code .repository}, controllers are placed in the sibling
 * {@code .controller} package; otherwise in the repository's own package. A hand-written class with
 * the same fully qualified name replaces the generated one.
 *
 * <pre>{@code
 * @FreezerplateApi(request = BookRequest.class)
 * public interface BookRepository extends JpaRepository<Book, Long> {}
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FreezerplateApi {

  /**
   * The path of the endpoints, for example {@code "categories"} for {@code /categories}, or {@code
   * /api/categories} when {@code freezerplate.base-path} is {@code /api}.
   *
   * <p>Leading and trailing slashes are optional; nested paths such as {@code "library/books"} are
   * allowed. The last segment also names the list operation in the OpenAPI document, so {@code
   * "categories"} produces {@code getCategories} and {@code "book-shelves"} produces {@code
   * getBookShelves}. When left empty, the path is the entity name in lower case with an {@code s}
   * appended, which suits regular plurals only.
   *
   * @return the path, or an empty string for the default
   */
  String path() default "";

  /**
   * The type returned instead of the entity in all responses for this entity.
   *
   * <p>The application must provide a bean implementing {@code
   * io.github.mtsdurica.freezerplate.web.FreezerplateMapper<Entity, Dto>}. When left unset, the DTO
   * generated from {@link FreezerplatePublic} fields is used if the entity has any, otherwise the
   * entity itself.
   *
   * @return the response DTO type, or {@code void} for the default
   */
  Class<?> response() default void.class;

  /**
   * The type accepted instead of the entity in {@code POST}, {@code PUT} and {@code PATCH} bodies.
   *
   * <p>The application must provide a bean implementing {@code
   * io.github.mtsdurica.freezerplate.web.FreezerplateRequestMapper<Request, Entity>}. {@code POST}
   * and {@code PUT} bodies are validated with Bean Validation; {@code PATCH} bodies are JSON Merge
   * Patches (RFC 7396) applied to the current state and validated after merging. When left unset,
   * request bodies use the entity's fields, with related entities referenced by id.
   *
   * @return the request DTO type, or {@code void} to accept the entity's fields
   */
  Class<?> request() default void.class;
}
