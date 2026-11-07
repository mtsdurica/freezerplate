# Freezerplate – Features

Freezerplate generates REST CRUD endpoints for Spring Data JPA repositories at compile time. You annotate a repository, and an annotation processor writes ordinary Spring MVC controllers for it.

## Requirements

- Java 17+ (the same minimum as Spring Boot; tested on 17 and 25)
- Spring Boot 4.1 (Spring Framework 7, Jackson 3)
- Spring Data JPA, Spring MVC, Bean Validation
- springdoc-openapi 3.1, pulled in by Freezerplate itself
- Entities using field access (JPA annotations on fields)

## Setup

- Add the `freezerplate` dependency.
- Add `freezerplate-processor` to the compiler plugin's `annotationProcessorPaths`.
- Annotate a repository with `@FreezerplateApi`. It must directly extend `JpaRepository<Entity, Id>`.
- Auto-configuration is picked up by Spring Boot; there's nothing to enable.

## Generated CRUD endpoints

For each `@FreezerplateApi` repository, the processor generates `<Entity>FreezerplateController`.

| Method | Path | Status | Response |
|---|---|---|---|
| GET | `/<path>` | 200 | `{"items": [...]}` |
| GET | `/<path>/{id}` | 200 / 404 | `{"data": {...}}` |
| POST | `/<path>` | 201 | `{"data": {...}}` |
| PUT | `/<path>/{id}` | 200 / 404 | `{"data": {...}}` |
| PATCH | `/<path>/{id}` | 200 / 404 | `{"data": {...}}` |
| DELETE | `/<path>/{id}` | 204 / 404 | – |

- **Path:** set with `@FreezerplateApi(path = "categories")`. Leading and trailing slashes are optional, and nested paths like `library/books` work. Without it, the path is the lower-case entity name plus "s", e.g. `Book` → `/books`. An invalid path (empty, whitespace, `{placeholders}`) is a compile error.
- **Base path:** none by default, so endpoints are served at the root (`/books`), like Spring Data REST. Setting `freezerplate.base-path=/api` prefixes all generated endpoints (`/api/books`) and only those, not hand-written controllers. Slashes are normalized, so `api`, `/api` and `/api/` are equivalent. Without a prefix, a hand-written controller on the same path makes Spring fail at startup with an "ambiguous mapping" error. The examples below assume `freezerplate.base-path=/api`.
- **Package:** if the repository package ends in `.repository`, controllers go into the sibling `.controller` package. Otherwise they go into the repository's own package.
- **Validation:** POST and PUT bodies are validated with `@Valid`.
- **Response wrappers:** single items are wrapped in `ResourceResponse` (`data`), lists in `CollectionResponse` (`items`). The OpenAPI schemas are named accordingly, e.g. `ResourceResponseBook`.

## Relations

### Reading
- **Nesting:** related entities are embedded in responses.
- **Back-reference trimming:** in a bidirectional relation, the nested side leaves out the reference back to the parent. A book's authors don't list their books, and a review's book doesn't list its reviews.
- **Both sides detected:** this works from either side, by `mappedBy` on the inverse side or by looking up the inverse side from the owning side. It covers `@OneToMany`, `@ManyToOne`, `@ManyToMany` and `@OneToOne`.

### Writing (Spring Data REST semantics, plain IDs)
- **Owning-side relations are written by ID:**
  - single: `"book": 3`
  - collection: `"authors": [1, 2]`
- **Inverse-side relations** (`mappedBy`) are read-only and ignored in request bodies.

| Request | Owning-side relations |
|---|---|
| POST | Set from the IDs given. A missing collection becomes empty. |
| PUT | Kept from the stored entity; relation values in the body are ignored. Plain fields are replaced. |
| PATCH | Replaced if present, unchanged if absent. |

- **Unknown IDs** in a body give `400` with a message such as "No Author with id 999".
- **Constructors:** entities whose constructor takes a collection relation work. An absent or `null` collection is passed as empty.

### Association endpoints
For every collection relation on the owning side (`@OneToMany` / `@ManyToMany` without `mappedBy`) whose target also has a `@FreezerplateApi` repository, the processor generates `<Entity><Field>AssociationController`.

| Method | Path | Body | Effect |
|---|---|---|---|
| GET | `/api/books/{id}/authors` | – | Lists the related entities |
| PUT | `/api/books/{id}/authors` | `[1, 2]` | Replaces the whole set |
| POST | `/api/books/{id}/authors` | `[3]` | Adds to the set |
| DELETE | `/api/books/{id}/authors/{targetId}` | – | Removes one |

- **Paths:** association endpoints follow the owner's path, e.g. `/api/categories/{id}/books` for `@FreezerplateApi(path = "categories")`.
- **Responses:** PUT, POST and DELETE return `204`.
- **Errors:** an unknown owner gives `404`; unknown target IDs give `400`.

## Response DTOs

### Generated DTOs (`@FreezerplatePublic`)
- **What's generated:** marking entity fields with `@FreezerplatePublic` generates `<Entity>Dto` and `<Entity>DtoMapper` (a Spring component) next to the entity.
- **Field selection:** only marked fields appear in responses.
- **Renaming:** `@FreezerplatePublic(name = "headline")` renames the field in the DTO.
- **Relations:** marked relation fields become nested DTOs, or lists of them, with the same back-reference trimming as entities.
- **Target needs a DTO:** a marked relation whose target entity has no marked fields is a compile error.
- **Usage:** CRUD and association controllers use the DTO automatically for all responses.

### Your own response DTO
- **Declaration:** `@FreezerplateApi(response = MyView.class)`, plus a Spring bean implementing `FreezerplateMapper<Entity, MyView>`.
- **Usage:** all responses for that entity go through your mapper, including association endpoints that list it.
- **Precedence:** it takes priority over a generated DTO for that entity's own controllers.

## Request DTOs

- **Declaration:** `@FreezerplateApi(request = MyRequest.class)`, plus a Spring bean implementing `FreezerplateRequestMapper<MyRequest, Entity>`:
  - `toEntity(request)` for POST.
  - `toRequest(entity)` provides the current state for PATCH.
  - `update(request, entity)` applies a complete request, for PUT and PATCH.
- **Relations:** the mapper resolves relation IDs itself. Throwing `UnknownReferenceException` gives `400`.

| Request | Behaviour | Validation |
|---|---|---|
| POST | `toEntity`, then save | `@Valid` on the request |
| PUT | load, `update`, save | `@Valid` on the request |
| PATCH | JSON Merge Patch on top of `toRequest(entity)`, then `update` | The merged request is validated; violations give `400` and nothing is saved |

- **Combining:** request DTOs combine freely with generated or custom response DTOs.

## PATCH semantics

- **Format:** JSON Merge Patch (RFC 7396). Absent fields are unchanged, `null` clears a field, nested objects merge recursively.
- **Without a request DTO:** the patch is merged directly into the entity, with relations by ID.
- **With a request DTO:** the patch is merged into the request DTO built from the current entity; see above.

## Error handling (RFC 9457 Problem Details)

- **Format:** every error from a Freezerplate controller is an `application/problem+json` document. It has `title`, `status`, `detail` and `instance` (the request path). `type` is omitted, which per the RFC means `about:blank`.
- **Field-level errors:** problems caused by specific request fields carry an `errors` extension. Each entry has a JSON Pointer to the field and a message: `{"pointer": "#/authors/1", "detail": "No Author with id -1"}`.
- **Scope:** hand-written controllers are unaffected. An application's own `@ControllerAdvice` beans take precedence.

| Situation | Status | `errors` |
|---|---|---|
| Entity not found | 404 | – |
| Malformed JSON | 400 | – |
| Missing request body | 400 | – |
| Unknown ID in an entity request body | 400 | Pointer to the reference, e.g. `#/book` |
| Invalid value type in a body | 400 | Pointer to the field |
| Path variable of the wrong type, e.g. `/books/abc` | 400 | – |
| Bean Validation failure (POST/PUT body, merged PATCH request) | 400 | One entry per violated field |
| Unknown ID in an association body or request mapper | 400 | – |
| Database constraint violation, e.g. deleting a book that still has reviews | 409 | – |
| Optimistic locking failure | 409 | – |
| Unsupported media type, method not allowed by the handler, etc. | Spring's standard status | – |
| Anything unexpected | 500 | –; the details are logged, not returned |

## JSON isolation

- **Separate mapper:** Freezerplate uses its own copy of the application's `JsonMapper`, so your Jackson settings apply.
- **Scope:** the relation handling (trimming, read-only inverse side, ID references) is active only for Freezerplate request bodies and `ResourceResponse` / `CollectionResponse` responses.
- **No global effect:** the application's own `JsonMapper` and hand-written controllers behave like stock Jackson.

## OpenAPI (springdoc)

- **Coverage:** all generated endpoints appear in `/v3/api-docs`.
- **Entity operation IDs:**
  - `get<Plural>`, with the plural taken from the path's last segment: `books` → `getBooks`, `categories` → `getCategories`, `book-shelves` → `getBookShelves`
  - `get<Entity>`
  - `create<Entity>`
  - `update<Entity>`
  - `patch<Entity>`
  - `delete<Entity>`
- **Association operation IDs:**
  - `get<Entity><Field>`
  - `replace<Entity><Field>`
  - `add<Entity><Field>`
  - `remove<Entity><Field>`
- **Status codes:** 201 and 204 are declared where they apply.
- **Success content type:** success responses are declared as `application/json`.
- **Error responses:** each operation documents its possible errors as `application/problem+json`, referencing a shared `ProblemDetail` schema with the RFC 9457 members and the `errors` extension.

  | Operation | Documented errors |
  |---|---|
  | GET collection | 500 |
  | GET one | 400, 404, 500 |
  | POST | 400, 409, 500 |
  | PUT, PATCH | 400, 404, 409, 500 |
  | DELETE | 400, 404, 409, 500 |
  | Association endpoints | Same rules |

  The rules:
  - **400:** the operation takes a body or a path variable.
  - **404:** it addresses an existing entity by path.
  - **409:** it modifies data.
  - **500:** always.
- **Request bodies:** the spec describes exactly what the server accepts.
  - **Entity bodies:** POST and PUT use a published `<Entity>Request` schema:
    - `id` is omitted.
    - Inverse-side relations are omitted.
    - Owning-side relations are typed as an ID or a list of IDs, using the target entity's id type.
  - **Request DTOs:** POST and PUT use the DTO's own schema.
  - **PATCH:** uses a `<Request>Patch` variant, e.g. `BookRequestPatch` or `MagazineRequestPatch`, described as JSON Merge Patch. Nothing in it is required, and every field also accepts `null`.
  - **Hidden entity schemas:** an entity schema that would only appear in the spec because of a request body is not published. This keeps fields hidden by response DTOs out of the spec.
- **Client generators:** generators produce correct types. Verified with Orval 8 (React Query, strict TypeScript): correct requests compile, and nested objects where IDs are expected are rejected.

## Customisation

- **Replacing generated code:** a class with the same fully qualified name as a generated controller, DTO or mapper replaces it; the processor skips generating it.
- **Replacing beans:** all Freezerplate beans are `@ConditionalOnMissingBean`. In practice only the ones with public types can be replaced: the JSON mapper, the entity helper and the request patcher. These live in the `internal` package, so they aren't a stable API. The MVC configurer, exception handler and OpenAPI customizers have package-private types, so an application can't declare a replacement.
- **Base path:** `freezerplate.base-path`.

## Documentation and IDE support

- **Javadoc:** the public API has Javadoc, covering the annotations, mapper interfaces, response wrappers, exceptions, configuration properties and auto-configuration. It is published as a `-javadoc.jar`. The build fails on missing or broken Javadoc there. The `internal` package is documented as not being API.
- **Configuration metadata:** IDEs describe and auto-complete `freezerplate.base-path`.

## Known limitations

- **Plain lists:** no pagination, sorting or filtering yet; `GET` on a collection returns everything.
- **Silent PUT behaviour:** PUT without a request DTO ignores relation values in the body.
- **No entity validation on PATCH:** without a request DTO, PATCH isn't validated.
- **Only direct cycles trimmed:** back-reference trimming handles direct bidirectional pairs, not longer cycles (A → B → C → A).
- **Custom mappers in nested DTOs:** a hand-written `<Entity>DtoMapper` nested by generated mappers must provide `static map(entity, String skippedField)`.
- **Getter naming:** generated mappers assume `getX()`, so `boolean isX()` getters are not supported.
- **Empty single relations:** generated DTOs omit an empty single relation instead of returning `null`.
- **Default plural:** without `path`, the plural just adds "s"; irregular plurals need an explicit `path`.
- **No JSON Patch:** JSON Patch (RFC 6902) is not supported.
- **Lazy loading:** nested lazy relations rely on Spring's open-in-view (on by default).
- **Kotlin:** untested.
- **Unconditional auto-configuration:** it doesn't check for a servlet web application or JPA, so an application without them fails at startup instead of Freezerplate backing off.
- **Duplicate paths:** two repositories with the same path aren't detected at compile time; Spring fails at startup with an "ambiguous mapping" error.
- **Generic 404 message:** "No entity found for id: 5" doesn't name the entity type.
- **Name clash:** `EntityNotFoundException` has the same simple name as `jakarta.persistence.EntityNotFoundException`.
- **No hooks:** there are no events or callbacks around create, update or delete; adding business rules means replacing the generated controller.
