package io.github.mtsdurica.freezerplate.processor;

import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.COLLECTION_RESPONSE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.DELETE_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.ENTITY_NOT_FOUND_EXCEPTION;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_ASSOCIATION;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_FOR;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_MAPPER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.GET_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.JPA_ENTITIES;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.POST_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.PUT_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REQUEST_MAPPING;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REST_CONTROLLER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.TRANSACTIONAL;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.ID;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.annotationWithValue;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.mapToResponse;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.pathVariable;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.privateFinalField;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.requestBody;
import static io.github.mtsdurica.freezerplate.processor.JavaPoetSpecs.responseStatus;
import static io.github.mtsdurica.freezerplate.processor.TypeMirrors.capitalize;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

final class AssociationGenerator {

  private static final String CONTROLLER_SUFFIX = "AssociationController";
  private static final String OWNER_REPOSITORY = "ownerRepository";
  private static final String TARGET_REPOSITORY = "targetRepository";
  private static final String ENTITIES = "entities";
  private static final String ASSOCIATION = "association";
  private static final String RESPONSE_MAPPER = "responseMapper";
  private static final String IDS = "ids";
  private static final String TARGET_ID = "targetId";

  private final ProcessingEnvironment processingEnvironment;
  private final SourceWriter sourceWriter;

  AssociationGenerator(ProcessingEnvironment processingEnvironment, SourceWriter sourceWriter) {
    this.processingEnvironment = processingEnvironment;
    this.sourceWriter = sourceWriter;
  }

  // Only owning-side collections get endpoints: JPA persists a relation from its owning side,
  // so changes made through the inverse side would silently not be saved.
  void generate(Map<String, RepositoryInfo> repositoriesByEntityName) {
    repositoriesByEntityName.forEach(
        (entityName, owner) ->
            Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(entityName))
                .ifPresent(
                    owningEntity ->
                        generateForOwningEntity(owningEntity, owner, repositoriesByEntityName)));
  }

  private void generateForOwningEntity(
      TypeElement owningEntity,
      RepositoryInfo owner,
      Map<String, RepositoryInfo> repositoriesByEntityName) {
    for (Element field : ElementFilter.fieldsIn(owningEntity.getEnclosedElements())) {
      if (!JpaAnnotations.isOwningCollectionRelation(field)) {
        continue;
      }
      TypeMirrors.unwrapCollectionElement(field.asType())
          .map(elementType -> repositoriesByEntityName.get(elementType.toString()))
          .map(target -> new Association(owner, field.getSimpleName().toString(), target))
          .ifPresent(association -> generateController(association, owningEntity));
    }
  }

  private void generateController(Association association, TypeElement owningEntity) {
    ClassName controller =
        ClassName.get(association.getOwner().getControllerPackage(), association.getControllerName());
    if (sourceWriter.existsAlready(controller)) {
      return;
    }
    TypeSpec.Builder controllerBuilder =
        TypeSpec.classBuilder(controller)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(GeneratedMarker.annotation())
            .addAnnotation(REST_CONTROLLER)
            .addAnnotation(annotationWithValue(REQUEST_MAPPING, association.getPath()))
            .addAnnotation(freezerplateFor(association))
            .addField(privateFinalField(association.getOwner().getRepositoryType(), OWNER_REPOSITORY))
            .addField(privateFinalField(associationType(association), ASSOCIATION));
    if (association.getTarget().hasResponseDto()) {
      controllerBuilder.addField(privateFinalField(responseMapperType(association), RESPONSE_MAPPER));
    }
    controllerBuilder
        .addMethod(buildConstructor(association))
        .addMethod(buildFindAll(association))
        .addMethod(buildReplace(association))
        .addMethod(buildAdd(association))
        .addMethod(buildRemove(association));
    sourceWriter.write(controller, controllerBuilder.build(), owningEntity);
  }

  private static AnnotationSpec freezerplateFor(Association association) {
    return AnnotationSpec.builder(FREEZERPLATE_FOR)
        .addMember("value", "$T.class", association.getOwner().getEntityType())
        .addMember(ASSOCIATION, "$S", association.getFieldName())
        .build();
  }

  private static MethodSpec buildConstructor(Association association) {
    MethodSpec.Builder constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(association.getOwner().getRepositoryType(), OWNER_REPOSITORY)
            .addParameter(association.getTarget().getRepositoryType(), TARGET_REPOSITORY)
            .addParameter(JPA_ENTITIES, ENTITIES)
            .addStatement("this.$N = $N", OWNER_REPOSITORY, OWNER_REPOSITORY)
            .addStatement(
                "this.$N = new $T<>($N, $N)",
                ASSOCIATION,
                FREEZERPLATE_ASSOCIATION,
                TARGET_REPOSITORY,
                ENTITIES);
    if (association.getTarget().hasResponseDto()) {
      constructor
          .addParameter(responseMapperType(association), RESPONSE_MAPPER)
          .addStatement("this.$N = $N", RESPONSE_MAPPER, RESPONSE_MAPPER);
    }
    return constructor.build();
  }

  private static MethodSpec buildFindAll(Association association) {
    return MethodSpec.methodBuilder("findAll")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(GET_MAPPING)
        .returns(
            ParameterizedTypeName.get(COLLECTION_RESPONSE, association.getTarget().getResponseType()))
        .addParameter(ownerIdParameter(association))
        .addStatement(loadOwner(association))
        .addStatement(
            "return association.findAll(owner::$N)$L",
            association.getGetterName(),
            mapToResponse(association.getTarget().hasResponseDto()))
        .build();
  }

  private static MethodSpec buildReplace(Association association) {
    return mutation("replace", AnnotationSpec.builder(PUT_MAPPING).build(), association)
        .addParameter(targetIdsBody(association))
        .addStatement(loadOwner(association))
        .addStatement("association.replace(owner::$N, $N)", association.getSetterName(), IDS)
        .addStatement("$N.save(owner)", OWNER_REPOSITORY)
        .build();
  }

  private static MethodSpec buildAdd(Association association) {
    return mutation("add", AnnotationSpec.builder(POST_MAPPING).build(), association)
        .addParameter(targetIdsBody(association))
        .addStatement(loadOwner(association))
        .addStatement(
            "association.add(owner::$N, owner::$N, $N)",
            association.getGetterName(),
            association.getSetterName(),
            IDS)
        .addStatement("$N.save(owner)", OWNER_REPOSITORY)
        .build();
  }

  private static MethodSpec buildRemove(Association association) {
    return mutation("remove", annotationWithValue(DELETE_MAPPING, "/{targetId}"), association)
        .addParameter(pathVariable(association.getTarget().getIdType(), TARGET_ID))
        .addStatement(loadOwner(association))
        .addStatement(
            "association.remove(owner::$N, owner::$N, $N)",
            association.getGetterName(),
            association.getSetterName(),
            TARGET_ID)
        .addStatement("$N.save(owner)", OWNER_REPOSITORY)
        .build();
  }

  private static MethodSpec.Builder mutation(
      String name, AnnotationSpec mapping, Association association) {
    return MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(mapping)
        .addAnnotation(TRANSACTIONAL)
        .addAnnotation(responseStatus("NO_CONTENT"))
        .addParameter(ownerIdParameter(association));
  }

  private static CodeBlock loadOwner(Association association) {
    return CodeBlock.of(
        "$T owner = $N.findById(id).orElseThrow(() -> new $T(id))",
        association.getOwner().getEntityType(),
        OWNER_REPOSITORY,
        ENTITY_NOT_FOUND_EXCEPTION);
  }

  private static ParameterSpec ownerIdParameter(Association association) {
    return pathVariable(association.getOwner().getIdType(), ID);
  }

  private static ParameterSpec targetIdsBody(Association association) {
    return requestBody(
        ParameterizedTypeName.get(ClassName.get(List.class), association.getTarget().getIdType()),
        IDS);
  }

  private static TypeName associationType(Association association) {
    RepositoryInfo target = association.getTarget();
    return ParameterizedTypeName.get(
        FREEZERPLATE_ASSOCIATION,
        target.getEntityType(),
        target.getIdType(),
        target.getRepositoryType());
  }

  private static TypeName responseMapperType(Association association) {
    RepositoryInfo target = association.getTarget();
    return ParameterizedTypeName.get(
        FREEZERPLATE_MAPPER, target.getEntityType(), target.getResponseType());
  }

  private static final class Association {

    private final RepositoryInfo owner;
    private final String fieldName;
    private final RepositoryInfo target;

    private Association(RepositoryInfo owner, String fieldName, RepositoryInfo target) {
      this.owner = owner;
      this.fieldName = fieldName;
      this.target = target;
    }

    RepositoryInfo getOwner() {
      return owner;
    }

    String getFieldName() {
      return fieldName;
    }

    RepositoryInfo getTarget() {
      return target;
    }

    String getControllerName() {
      return owner.getEntityName() + capitalize(fieldName) + CONTROLLER_SUFFIX;
    }

    String getPath() {
      return owner.getPath() + "/{id}/" + fieldName;
    }

    String getGetterName() {
      return "get" + capitalize(fieldName);
    }

    String getSetterName() {
      return "set" + capitalize(fieldName);
    }
  }
}
