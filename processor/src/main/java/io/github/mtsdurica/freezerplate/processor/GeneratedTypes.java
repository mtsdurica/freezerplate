package io.github.mtsdurica.freezerplate.processor;

import com.squareup.javapoet.ClassName;

/**
 * Types are referenced by name instead of by class literal so the processor does not need
 * Spring, Jackson or springdoc on its own classpath; only the generated code does.
 */
final class GeneratedTypes {

  private static final String FREEZERPLATE_INTERNAL = "io.github.mtsdurica.freezerplate.internal";
  private static final String FREEZERPLATE_MODEL = "io.github.mtsdurica.freezerplate.model";
  private static final String FREEZERPLATE_WEB = "io.github.mtsdurica.freezerplate.web";
  private static final String SPRING_WEB_BIND = "org.springframework.web.bind.annotation";
  private static final String JACKSON_ANNOTATION = "com.fasterxml.jackson.annotation";

  static final ClassName FREEZERPLATE_CRUD = ClassName.get(FREEZERPLATE_INTERNAL, "FreezerplateCrud");
  static final ClassName FREEZERPLATE_ASSOCIATION =
      ClassName.get(FREEZERPLATE_INTERNAL, "FreezerplateAssociation");
  static final ClassName FREEZERPLATE_FOR = ClassName.get(FREEZERPLATE_INTERNAL, "FreezerplateFor");
  static final ClassName FREEZERPLATE_JSON_MAPPER =
      ClassName.get(FREEZERPLATE_INTERNAL, "FreezerplateJsonMapper");
  static final ClassName JPA_ENTITIES = ClassName.get(FREEZERPLATE_INTERNAL, "JpaEntities");
  static final ClassName REQUEST_PATCHER = ClassName.get(FREEZERPLATE_INTERNAL, "RequestPatcher");
  static final ClassName COLLECTION_RESPONSE =
      ClassName.get(FREEZERPLATE_MODEL, "CollectionResponse");
  static final ClassName RESOURCE_RESPONSE =
      ClassName.get(FREEZERPLATE_MODEL, "ResourceResponse");
  static final ClassName FREEZERPLATE_MAPPER = ClassName.get(FREEZERPLATE_WEB, "FreezerplateMapper");
  static final ClassName FREEZERPLATE_REQUEST_MAPPER =
      ClassName.get(FREEZERPLATE_WEB, "FreezerplateRequestMapper");
  static final ClassName ENTITY_NOT_FOUND_EXCEPTION =
      ClassName.get(FREEZERPLATE_WEB, "EntityNotFoundException");

  static final ClassName REST_CONTROLLER = ClassName.get(SPRING_WEB_BIND, "RestController");
  static final ClassName REQUEST_MAPPING = ClassName.get(SPRING_WEB_BIND, "RequestMapping");
  static final ClassName GET_MAPPING = ClassName.get(SPRING_WEB_BIND, "GetMapping");
  static final ClassName POST_MAPPING = ClassName.get(SPRING_WEB_BIND, "PostMapping");
  static final ClassName PUT_MAPPING = ClassName.get(SPRING_WEB_BIND, "PutMapping");
  static final ClassName PATCH_MAPPING = ClassName.get(SPRING_WEB_BIND, "PatchMapping");
  static final ClassName DELETE_MAPPING = ClassName.get(SPRING_WEB_BIND, "DeleteMapping");
  static final ClassName PATH_VARIABLE = ClassName.get(SPRING_WEB_BIND, "PathVariable");
  static final ClassName REQUEST_BODY = ClassName.get(SPRING_WEB_BIND, "RequestBody");
  static final ClassName RESPONSE_STATUS = ClassName.get(SPRING_WEB_BIND, "ResponseStatus");
  static final ClassName HTTP_STATUS = ClassName.get("org.springframework.http", "HttpStatus");
  static final ClassName TRANSACTIONAL =
      ClassName.get("org.springframework.transaction.annotation", "Transactional");
  static final ClassName SPRING_COMPONENT =
      ClassName.get("org.springframework.stereotype", "Component");
  static final ClassName VALID = ClassName.get("jakarta.validation", "Valid");
  static final ClassName SCHEMA = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");
  static final ClassName JSON_NODE = ClassName.get("tools.jackson.databind", "JsonNode");
  static final ClassName JSON_INCLUDE = ClassName.get(JACKSON_ANNOTATION, "JsonInclude");
  static final ClassName JSON_INCLUDE_RULE =
      ClassName.get(JACKSON_ANNOTATION, "JsonInclude", "Include");
  static final ClassName GENERATED = ClassName.get("javax.annotation.processing", "Generated");

  private GeneratedTypes() {}
}
