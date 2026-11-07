package io.github.mtsdurica.freezerplate.processor;

import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.COLLECTION_RESPONSE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.DELETE_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.RESOURCE_RESPONSE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_CRUD;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_FOR;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_JSON_MAPPER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_MAPPER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_REQUEST_MAPPER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.GET_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.JPA_ENTITIES;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.JSON_NODE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.PATCH_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.POST_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.PUT_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REQUEST_BODY;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REQUEST_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REQUEST_PATCHER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REST_CONTROLLER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.SCHEMA;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.TRANSACTIONAL;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.ID;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.ID_PATH;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.annotationWithClass;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.annotationWithValue;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.mapToResponse;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.pathVariable;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.privateFinalField;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.responseStatus;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.validatedRequestBody;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

final class ControllerGenerator {

  private static final String CONTROLLER_SUFFIX = "FreezerplateController";
  private static final String CRUD = "crud";
  private static final String RESPONSE_MAPPER = "responseMapper";
  private static final String REQUEST_MAPPER = "requestMapper";
  private static final String REQUEST_PATCHER_FIELD = "requestPatcher";
  private static final String REQUEST = "request";
  private static final String ENTITY = "entity";
  private static final String PATCH = "patch";

  private final SourceWriter sourceWriter;

  ControllerGenerator(SourceWriter sourceWriter) {
    this.sourceWriter = sourceWriter;
  }

  void generate(RepositoryInfo repository) {
    ClassName controller =
        ClassName.get(
            repository.getControllerPackage(), repository.getEntityName() + CONTROLLER_SUFFIX);
    if (sourceWriter.existsAlready(controller)) {
      return;
    }

    TypeSpec.Builder controllerBuilder =
        TypeSpec.classBuilder(controller)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(GeneratedMarker.annotation())
            .addAnnotation(REST_CONTROLLER)
            .addAnnotation(annotationWithValue(REQUEST_MAPPING, repository.getPath()))
            .addAnnotation(annotationWithClass(FREEZERPLATE_FOR, repository.getEntityType()))
            .addField(privateFinalField(crudType(repository), CRUD));
    MethodSpec.Builder constructorBuilder = crudConstructor(repository);
    addResponseMapper(repository, controllerBuilder, constructorBuilder);
    addRequestMapper(repository, controllerBuilder, constructorBuilder);

    controllerBuilder
        .addMethod(constructorBuilder.build())
        .addMethod(buildFindAll(repository))
        .addMethod(buildFindOne(repository))
        .addMethod(buildCreate(repository))
        .addMethod(buildUpdate(repository))
        .addMethod(buildPatch(repository))
        .addMethod(buildDelete(repository));

    sourceWriter.write(controller, controllerBuilder.build(), repository.getRepositoryElement());
  }

  private static TypeName crudType(RepositoryInfo repository) {
    return ParameterizedTypeName.get(
        FREEZERPLATE_CRUD,
        repository.getEntityType(),
        repository.getIdType(),
        repository.getRepositoryType());
  }

  private static MethodSpec.Builder crudConstructor(RepositoryInfo repository) {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(repository.getRepositoryType(), "repository")
        .addParameter(FREEZERPLATE_JSON_MAPPER, "jsonMapper")
        .addParameter(JPA_ENTITIES, "entities")
        .addStatement("this.$N = new $T<>(repository, jsonMapper, entities)", CRUD, FREEZERPLATE_CRUD);
  }

  private static void addResponseMapper(
      RepositoryInfo repository,
      TypeSpec.Builder controllerBuilder,
      MethodSpec.Builder constructorBuilder) {
    if (!repository.hasResponseDto()) {
      return;
    }
    TypeName mapperType =
        ParameterizedTypeName.get(
            FREEZERPLATE_MAPPER, repository.getEntityType(), repository.getResponseType());
    addInjectedField(controllerBuilder, constructorBuilder, mapperType, RESPONSE_MAPPER);
  }

  private static void addRequestMapper(
      RepositoryInfo repository,
      TypeSpec.Builder controllerBuilder,
      MethodSpec.Builder constructorBuilder) {
    repository
        .getRequestType()
        .ifPresent(
            requestType -> {
              TypeName mapperType =
                  ParameterizedTypeName.get(
                      FREEZERPLATE_REQUEST_MAPPER, requestType, repository.getEntityType());
              addInjectedField(controllerBuilder, constructorBuilder, mapperType, REQUEST_MAPPER);
              addInjectedField(
                  controllerBuilder, constructorBuilder, REQUEST_PATCHER, REQUEST_PATCHER_FIELD);
            });
  }

  private static void addInjectedField(
      TypeSpec.Builder controllerBuilder,
      MethodSpec.Builder constructorBuilder,
      TypeName type,
      String name) {
    controllerBuilder.addField(privateFinalField(type, name));
    constructorBuilder.addParameter(type, name).addStatement("this.$N = $N", name, name);
  }

  private static MethodSpec buildFindAll(RepositoryInfo repository) {
    return MethodSpec.methodBuilder("findAll")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(GET_MAPPING)
        .returns(ParameterizedTypeName.get(COLLECTION_RESPONSE, repository.getResponseType()))
        .addStatement("return crud.findAll()$L", mapToResponse(repository.hasResponseDto()))
        .build();
  }

  private static MethodSpec buildFindOne(RepositoryInfo repository) {
    return MethodSpec.methodBuilder("findOne")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotationWithValue(GET_MAPPING, ID_PATH))
        .returns(resourceResponseOf(repository))
        .addParameter(idParameter(repository))
        .addStatement("return crud.findOne(id)$L", mapToResponse(repository.hasResponseDto()))
        .build();
  }

  private static MethodSpec buildCreate(RepositoryInfo repository) {
    String entity = repository.hasRequestDto() ? "requestMapper.toEntity(request)" : ENTITY;
    return MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(POST_MAPPING)
        .addAnnotation(responseStatus("CREATED"))
        .returns(resourceResponseOf(repository))
        .addParameter(validatedBody(repository))
        .addStatement(
            "return crud.create($L)$L", entity, mapToResponse(repository.hasResponseDto()))
        .build();
  }

  private static MethodSpec buildUpdate(RepositoryInfo repository) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("update")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(annotationWithValue(PUT_MAPPING, ID_PATH))
            .returns(resourceResponseOf(repository))
            .addParameter(idParameter(repository))
            .addParameter(validatedBody(repository));
    if (repository.hasRequestDto()) {
      return builder
          .addAnnotation(TRANSACTIONAL)
          .addStatement(
              "return crud.modify(id, entity -> requestMapper.update(request, entity))$L",
              mapToResponse(repository.hasResponseDto()))
          .build();
    }
    return builder
        .addStatement("return crud.update(id, entity)$L", mapToResponse(repository.hasResponseDto()))
        .build();
  }

  private static MethodSpec buildPatch(RepositoryInfo repository) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder(PATCH)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(annotationWithValue(PATCH_MAPPING, ID_PATH))
            .addAnnotation(TRANSACTIONAL)
            .returns(resourceResponseOf(repository))
            .addParameter(idParameter(repository))
            .addParameter(mergePatchBody(repository));
    String patchCall =
        repository.hasRequestDto()
            ? "crud.modify(id, entity -> requestPatcher.apply(entity, patch, requestMapper))"
            : "crud.patch(id, patch)";
    return builder
        .addStatement("return $L$L", patchCall, mapToResponse(repository.hasResponseDto()))
        .build();
  }

  private static MethodSpec buildDelete(RepositoryInfo repository) {
    return MethodSpec.methodBuilder("delete")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotationWithValue(DELETE_MAPPING, ID_PATH))
        .addAnnotation(responseStatus("NO_CONTENT"))
        .returns(TypeName.VOID)
        .addParameter(idParameter(repository))
        .addStatement("crud.delete(id)")
        .build();
  }

  // PATCH takes raw JSON so absent fields can be told apart from explicit nulls (RFC 7396);
  // the schema annotation keeps the OpenAPI document typed despite the JsonNode parameter.
  private static ParameterSpec mergePatchBody(RepositoryInfo repository) {
    AnnotationSpec.Builder schema =
        AnnotationSpec.builder(SCHEMA).addMember("description", "$S", "JSON Merge Patch (RFC 7396)");
    repository
        .getRequestType()
        .ifPresentOrElse(
            requestType -> schema.addMember("implementation", "$T.class", requestType),
            () -> schema.addMember("type", "$S", "object"));
    return ParameterSpec.builder(JSON_NODE, PATCH)
        .addAnnotation(REQUEST_BODY)
        .addAnnotation(schema.build())
        .build();
  }

  private static ParameterSpec validatedBody(RepositoryInfo repository) {
    return repository
        .getRequestType()
        .map(requestType -> validatedRequestBody(requestType, REQUEST))
        .orElseGet(() -> validatedRequestBody(repository.getEntityType(), ENTITY));
  }

  private static ParameterSpec idParameter(RepositoryInfo repository) {
    return pathVariable(repository.getIdType(), ID);
  }

  private static TypeName resourceResponseOf(RepositoryInfo repository) {
    return ParameterizedTypeName.get(RESOURCE_RESPONSE, repository.getResponseType());
  }
}
