package io.github.mtsdurica.freezerplate.autoconfigure;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.github.mtsdurica.freezerplate.internal.JpaEntities;
import io.github.mtsdurica.freezerplate.internal.JpaEntities.Relation;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

/**
 * Publishes request schemas that match what the API accepts (ids instead of nested entities,
 * optional and nullable fields for merge patches) so client generators produce correct types.
 * Works in two phases because springdoc builds schemas while visiting operations but only
 * exposes the complete component set to OpenApiCustomizers afterwards.
 */
final class RequestSchemaCustomizer implements OperationCustomizer, OpenApiCustomizer {

  private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
  private static final String REQUEST_SUFFIX = "Request";
  private static final String PATCH_SUFFIX = "Patch";
  private static final String OPENAPI_31_PREFIX = "3.1";
  private static final Pattern SCHEMA_REF =
      Pattern.compile("\"" + Pattern.quote(SCHEMA_REF_PREFIX) + "([^\"]+)\"");

  private final JpaEntities entities;
  private final Map<String, Class<?>> entityRequestSchemas = new ConcurrentHashMap<>();
  private final Map<String, String> patchSchemaBases = new ConcurrentHashMap<>();
  private final Set<String> entitySchemaNames = ConcurrentHashMap.newKeySet();
  // Keyed by identity: request bodies with equal content (POST and PUT) must both be updated.
  private final Map<Content, String> pendingBodySchemas =
      Collections.synchronizedMap(new IdentityHashMap<>());

  RequestSchemaCustomizer(JpaEntities entities) {
    this.entities = entities;
  }

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    FreezerplateFor freezerplateFor =
        handlerMethod.getBeanType().getAnnotation(FreezerplateFor.class);
    if (freezerplateFor == null) {
      return operation;
    }
    Class<?> entity = freezerplateFor.value();
    entitySchemaNames.add(entity.getSimpleName());
    if (!freezerplateFor.association().isEmpty() || operation.getRequestBody() == null) {
      return operation;
    }
    Content content = operation.getRequestBody().getContent();
    String entityRequestSchema = entity.getSimpleName() + REQUEST_SUFFIX;
    Optional<String> bodySchema = bodySchemaName(content);

    switch (handlerMethod.getMethod().getName()) {
      case "create", "update" -> {
        if (bodySchema.filter(entity.getSimpleName()::equals).isPresent()) {
          entityRequestSchemas.put(entityRequestSchema, entity);
          pendingBodySchemas.put(content, entityRequestSchema);
        }
      }
      case "patch" -> {
        if (bodySchema.isEmpty()) {
          entityRequestSchemas.put(entityRequestSchema, entity);
        }
        String base = bodySchema.orElse(entityRequestSchema);
        patchSchemaBases.put(base + PATCH_SUFFIX, base);
        pendingBodySchemas.put(content, base + PATCH_SUFFIX);
      }
      default -> {}
    }
    return operation;
  }

  // Request bodies are re-pointed only here, after springdoc's cleanup of unused schemas has run;
  // re-pointing earlier would let that cleanup delete entity schemas the request schemas derive
  // from.
  @Override
  public void customise(OpenAPI openApi) {
    Components components = Optional.ofNullable(openApi.getComponents()).orElseGet(Components::new);
    openApi.setComponents(components);
    boolean openApi31 = Objects.toString(openApi.getOpenapi(), "").startsWith(OPENAPI_31_PREFIX);

    entityRequestSchemas.forEach((name, entity) ->
        Optional.ofNullable(schemasOf(components).get(entity.getSimpleName()))
            .ifPresent(entitySchema ->
                components.addSchemas(name, entityRequestSchema(entitySchema, entity, openApi31))));
    patchSchemaBases.forEach((name, base) ->
        Optional.ofNullable(schemasOf(components).get(base))
            .ifPresent(
                baseSchema -> components.addSchemas(name, patchSchema(baseSchema, openApi31))));
    synchronized (pendingBodySchemas) {
      pendingBodySchemas.forEach(RequestSchemaCustomizer::useSchema);
      pendingBodySchemas.clear();
    }
    removeUnreachableEntitySchemas(openApi);
  }

  // Entity schemas only used as request bodies would otherwise stay published and expose fields
  // that response DTOs deliberately hide. Reachability is computed from the operations because
  // bidirectional entities reference each other and would keep each other alive.
  private void removeUnreachableEntitySchemas(OpenAPI openApi) {
    Map<String, Schema<?>> schemas = schemasOf(openApi.getComponents());
    var reachable = new HashSet<String>();
    var pending = new ArrayDeque<String>(referencedSchemaNames(rootsOf(openApi)));
    while (!pending.isEmpty()) {
      String name = pending.pop();
      if (reachable.add(name) && schemas.containsKey(name)) {
        pending.addAll(referencedSchemaNames(schemas.get(name)));
      }
    }
    entitySchemaNames.stream().filter(name -> !reachable.contains(name)).forEach(schemas::remove);
  }

  private static List<Object> rootsOf(OpenAPI openApi) {
    Components components = openApi.getComponents();
    return Stream.of(
            openApi.getPaths(),
            components.getResponses(),
            components.getRequestBodies(),
            components.getParameters())
        .filter(Objects::nonNull)
        .map(Object.class::cast)
        .toList();
  }

  private static Set<String> referencedSchemaNames(Object node) {
    try {
      Matcher matcher = SCHEMA_REF.matcher(Json31.mapper().writeValueAsString(node));
      var names = new HashSet<String>();
      while (matcher.find()) {
        names.add(matcher.group(1));
      }
      return names;
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not inspect OpenAPI schema references", exception);
    }
  }

  private Schema<?> entityRequestSchema(
      Schema<?> entitySchema, Class<?> entity, boolean openApi31) {
    String idName = entities.idNameOf(entity);
    Map<String, Relation> relationsByName =
        entities.relationsOf(entity).stream()
            .collect(Collectors.toMap(Relation::getName, Function.identity()));

    Schema<?> request =
        new ObjectSchema()
            .description(
                "Request body for "
                    + entity.getSimpleName()
                    + "; related entities are referenced by id");
    propertiesOf(entitySchema)
        .forEach((name, property) -> {
          Optional<Relation> relation = Optional.ofNullable(relationsByName.get(name));
          boolean ignored =
              name.equals(idName) || relation.filter(found -> !found.isOwning()).isPresent();
          if (!ignored) {
            request.addProperty(
                name,
                relation
                    .<Schema<?>>map(found -> referenceSchema(found, openApi31))
                    .orElse(property));
          }
        });
    List<String> required =
        Optional.ofNullable(entitySchema.getRequired()).orElseGet(List::of).stream()
            .filter(propertiesOf(request)::containsKey)
            .toList();
    if (!required.isEmpty()) {
      request.setRequired(required);
    }
    return request;
  }

  private static Schema<?> referenceSchema(Relation relation, boolean openApi31) {
    Schema<?> id =
        Optional.ofNullable(PrimitiveType.fromType(relation.getTargetIdType()))
            .<Schema<?>>map(
                primitive -> openApi31 ? primitive.createProperty31() : primitive.createProperty())
            .orElseGet(StringSchema::new);
    return relation.isCollection() ? new ArraySchema().items(id) : id;
  }

  private static Schema<?> patchSchema(Schema<?> base, boolean openApi31) {
    Schema<?> patch =
        new ObjectSchema()
            .description(
                "JSON Merge Patch (RFC 7396): absent fields are kept, null clears a field");
    propertiesOf(base)
        .forEach((name, property) -> patch.addProperty(name, nullable(property, openApi31)));
    return patch;
  }

  // OpenAPI 3.1 expresses "or null" with a null type; 3.0 only has the nullable flag.
  private static Schema<?> nullable(Schema<?> property, boolean openApi31) {
    Schema<Object> wrapper = new Schema<>();
    if (openApi31) {
      Schema<Object> nullType = new Schema<>();
      nullType.setSpecVersion(SpecVersion.V31);
      nullType.setTypes(Set.of("null"));
      wrapper.setSpecVersion(SpecVersion.V31);
      wrapper.setAnyOf(List.of(property, nullType));
    } else {
      wrapper.setNullable(true);
      wrapper.setAllOf(List.of(property));
    }
    return wrapper;
  }

  private static Optional<String> bodySchemaName(Content content) {
    return Optional.ofNullable(content).stream()
        .flatMap(mediaTypes -> mediaTypes.values().stream())
        .map(MediaType::getSchema)
        .filter(Objects::nonNull)
        .map(schema -> schema.get$ref())
        .filter(Objects::nonNull)
        .map(reference -> reference.substring(reference.lastIndexOf('/') + 1))
        .findFirst();
  }

  private static void useSchema(Content content, String schemaName) {
    content
        .values()
        .forEach(
            mediaType -> mediaType.setSchema(new Schema<>().$ref(SCHEMA_REF_PREFIX + schemaName)));
  }

  // swagger-models exposes schema maps as raw types; the unchecked view is confined to these
  // helpers.
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Map<String, Schema<?>> schemasOf(Components components) {
    if (components.getSchemas() == null) {
      components.setSchemas(new LinkedHashMap<>());
    }
    return (Map<String, Schema<?>>) (Map) components.getSchemas();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Map<String, Schema<?>> propertiesOf(Schema<?> schema) {
    return schema.getProperties() == null
        ? Map.of()
        : (Map<String, Schema<?>>) (Map) schema.getProperties();
  }
}
