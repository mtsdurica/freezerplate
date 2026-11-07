package io.github.mtsdurica.freezerplate.processor;

import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.FREEZERPLATE_MAPPER;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.JSON_INCLUDE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.JSON_INCLUDE_RULE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.SPRING_COMPONENT;
import static io.github.mtsdurica.freezerplate.processor.TypeMirrors.capitalize;
import static io.github.mtsdurica.freezerplate.processor.TypeMirrors.elementOrSelf;
import static io.github.mtsdurica.freezerplate.processor.TypeMirrors.isCollection;
import static io.github.mtsdurica.freezerplate.processor.TypeMirrors.qualifiedName;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.mtsdurica.freezerplate.annotation.FreezerplatePublic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

final class DtoGenerator {

  private static final String DTO_SUFFIX = "Dto";
  private static final String MAPPER_SUFFIX = "DtoMapper";
  private static final String ENTITY = "entity";
  private static final String SKIPPED_FIELD = "skippedField";

  private static final Set<String> SIMPLE_TYPES =
      Set.of(
          "java.lang.String",
          "java.lang.Boolean",
          "java.lang.Byte",
          "java.lang.Short",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Float",
          "java.lang.Double",
          "java.lang.Character",
          "java.math.BigDecimal",
          "java.math.BigInteger",
          "java.util.UUID",
          "java.time.LocalDate",
          "java.time.LocalDateTime",
          "java.time.LocalTime",
          "java.time.Instant",
          "java.time.OffsetDateTime",
          "java.time.ZonedDateTime");

  private final ProcessingEnvironment processingEnvironment;
  private final SourceWriter sourceWriter;

  DtoGenerator(ProcessingEnvironment processingEnvironment, SourceWriter sourceWriter) {
    this.processingEnvironment = processingEnvironment;
    this.sourceWriter = sourceWriter;
  }

  Map<String, ClassName> generate(RoundEnvironment roundEnvironment) {
    Map<TypeElement, List<VariableElement>> publicFieldsByEntity =
        collectPublicFields(roundEnvironment);
    Map<String, ClassName> dtoTypesByEntityName = dtoTypesOf(publicFieldsByEntity.keySet());
    publicFieldsByEntity.forEach(
        (entity, fields) -> {
          generateDto(entity, fields, dtoTypesByEntityName);
          generateMapper(entity, fields, dtoTypesByEntityName);
        });
    return dtoTypesByEntityName;
  }

  private Map<TypeElement, List<VariableElement>> collectPublicFields(
      RoundEnvironment roundEnvironment) {
    var publicFieldsByEntity = new LinkedHashMap<TypeElement, List<VariableElement>>();
    Set<? extends Element> annotated =
        roundEnvironment.getElementsAnnotatedWith(FreezerplatePublic.class);
    annotated.stream()
        .filter(element -> element.getKind() != ElementKind.FIELD)
        .forEach(
            element -> sourceWriter.error("@FreezerplatePublic only applies to entity fields", element));
    for (VariableElement field : ElementFilter.fieldsIn(annotated)) {
      publicFieldsByEntity
          .computeIfAbsent((TypeElement) field.getEnclosingElement(), entity -> new ArrayList<>())
          .add(field);
    }
    return publicFieldsByEntity;
  }

  // All DTO names are registered before any DTO is written, so relation fields can refer to
  // each other's DTOs regardless of the order in which entities are processed.
  private Map<String, ClassName> dtoTypesOf(Collection<TypeElement> entities) {
    var dtoTypesByEntityName = new LinkedHashMap<String, ClassName>();
    for (TypeElement entity : entities) {
      dtoTypesByEntityName.put(
          entity.getQualifiedName().toString(),
          ClassName.get(packageOf(entity), entity.getSimpleName() + DTO_SUFFIX));
    }
    return dtoTypesByEntityName;
  }

  private void generateDto(
      TypeElement entity, List<VariableElement> fields, Map<String, ClassName> dtoTypesByEntityName) {
    ClassName dtoType = dtoTypesByEntityName.get(entity.getQualifiedName().toString());
    if (sourceWriter.existsAlready(dtoType)) {
      return;
    }
    TypeSpec.Builder dtoBuilder =
        TypeSpec.classBuilder(dtoType)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(GeneratedMarker.annotation());
    MethodSpec.Builder allArgumentsConstructor =
        MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    for (VariableElement field : fields) {
      addDtoProperty(dtoBuilder, allArgumentsConstructor, field, dtoTypesByEntityName);
    }
    dtoBuilder
        .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build())
        .addMethod(allArgumentsConstructor.build());
    sourceWriter.write(dtoType, dtoBuilder.build(), entity);
  }

  private void addDtoProperty(
      TypeSpec.Builder dtoBuilder,
      MethodSpec.Builder allArgumentsConstructor,
      VariableElement field,
      Map<String, ClassName> dtoTypesByEntityName) {
    String name = publicName(field);
    TypeName type = dtoFieldType(field, dtoTypesByEntityName);
    FieldSpec.Builder dtoField = FieldSpec.builder(type, name, Modifier.PRIVATE);
    if (isDtoRelation(field, dtoTypesByEntityName)) {
      dtoField.addAnnotation(
          AnnotationSpec.builder(JSON_INCLUDE)
              .addMember("value", "$T.NON_NULL", JSON_INCLUDE_RULE)
              .build());
    }
    dtoBuilder
        .addField(dtoField.build())
        .addMethod(
            MethodSpec.methodBuilder("get" + capitalize(name))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return this.$N", name)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("set" + capitalize(name))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, name)
                .addStatement("this.$N = $N", name, name)
                .build());
    allArgumentsConstructor.addParameter(type, name).addStatement("this.$N = $N", name, name);
  }

  // Mappers call each other through static methods rather than injected beans: bidirectional
  // relations would otherwise form a circular bean dependency. The skipped field is the
  // back-reference, left out of a nested DTO the same way entity responses trim it.
  private void generateMapper(
      TypeElement entity, List<VariableElement> fields, Map<String, ClassName> dtoTypesByEntityName) {
    ClassName dtoType = dtoTypesByEntityName.get(entity.getQualifiedName().toString());
    ClassName mapperType = mapperTypeOf(dtoType.packageName(), entity);
    if (sourceWriter.existsAlready(mapperType)) {
      return;
    }
    ClassName entityType = ClassName.get(entity);
    List<CodeBlock> constructorArguments =
        fields.stream()
            .map(field -> mapperArgument(entity, field, dtoType.packageName(), dtoTypesByEntityName))
            .toList();

    MethodSpec toDto =
        MethodSpec.methodBuilder("toDto")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(dtoType)
            .addParameter(entityType, ENTITY)
            .addStatement("return map($N, null)", ENTITY)
            .build();
    MethodSpec map =
        MethodSpec.methodBuilder("map")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(dtoType)
            .addParameter(entityType, ENTITY)
            .addParameter(String.class, SKIPPED_FIELD)
            .beginControlFlow("if ($N == null)", ENTITY)
            .addStatement("return null")
            .endControlFlow()
            .addCode(
                "return new $T(\n$>$L$<\n);\n",
                dtoType,
                CodeBlock.join(constructorArguments, ",\n"))
            .build();

    TypeSpec mapper =
        TypeSpec.classBuilder(mapperType)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(GeneratedMarker.annotation())
            .addAnnotation(SPRING_COMPONENT)
            .addSuperinterface(ParameterizedTypeName.get(FREEZERPLATE_MAPPER, entityType, dtoType))
            .addMethod(toDto)
            .addMethod(map)
            .build();
    sourceWriter.write(mapperType, mapper, entity);
  }

  private CodeBlock mapperArgument(
      TypeElement owner,
      VariableElement field,
      String dtoPackage,
      Map<String, ClassName> dtoTypesByEntityName) {
    String fieldName = field.getSimpleName().toString();
    String getter = "get" + capitalize(fieldName);
    if (!isDtoRelation(field, dtoTypesByEntityName)) {
      return CodeBlock.of("$N.$N()", ENTITY, getter);
    }

    TypeElement target = (TypeElement) ((DeclaredType) elementOrSelf(field.asType())).asElement();
    ClassName targetMapper = mapperTypeOf(dtoPackage, target);
    CodeBlock backReference =
        backReferenceOf(field, owner, target)
            .map(name -> CodeBlock.of("$S", name))
            .orElseGet(() -> CodeBlock.of("null"));
    CodeBlock mapped =
        isCollection(field.asType())
            ? CodeBlock.of(
                "$N.$N().stream().map(item -> $T.map(item, $L)).toList()",
                ENTITY,
                getter,
                targetMapper,
                backReference)
            : CodeBlock.of("$T.map($N.$N(), $L)", targetMapper, ENTITY, getter, backReference);
    return CodeBlock.of("$S.equals($N) ? null : $L", fieldName, SKIPPED_FIELD, mapped);
  }

  private static Optional<String> backReferenceOf(
      VariableElement field, TypeElement owner, TypeElement target) {
    return JpaAnnotations.mappedBy(field)
        .or(
            () ->
                ElementFilter.fieldsIn(target.getEnclosedElements()).stream()
                    .filter(
                        candidate ->
                            JpaAnnotations.mappedBy(candidate)
                                .filter(field.getSimpleName()::contentEquals)
                                .isPresent())
                    .filter(
                        candidate ->
                            qualifiedName(elementOrSelf(candidate.asType()))
                                .equals(owner.getQualifiedName().toString()))
                    .map(candidate -> candidate.getSimpleName().toString())
                    .findFirst());
  }

  private boolean isDtoRelation(
      VariableElement field, Map<String, ClassName> dtoTypesByEntityName) {
    return dtoTypesByEntityName.containsKey(qualifiedName(elementOrSelf(field.asType())));
  }

  private TypeName dtoFieldType(
      VariableElement field, Map<String, ClassName> dtoTypesByEntityName) {
    TypeMirror targetType = elementOrSelf(field.asType());
    TypeName resolved = resolveTargetType(field, targetType, dtoTypesByEntityName);
    return isCollection(field.asType())
        ? ParameterizedTypeName.get(ClassName.get(List.class), resolved)
        : resolved;
  }

  private TypeName resolveTargetType(
      VariableElement field, TypeMirror targetType, Map<String, ClassName> dtoTypesByEntityName) {
    if (isPlainValue(targetType)) {
      return TypeName.get(targetType);
    }
    ClassName dtoType = dtoTypesByEntityName.get(qualifiedName(targetType));
    if (dtoType != null) {
      return dtoType;
    }
    sourceWriter.error(
        "@FreezerplatePublic field of type "
            + targetType
            + " has no corresponding DTO - mark at least one field on that type with "
            + "@FreezerplatePublic, or remove @FreezerplatePublic from this field.",
        field);
    return TypeName.get(targetType);
  }

  private static boolean isPlainValue(TypeMirror type) {
    return type.getKind().isPrimitive()
        || SIMPLE_TYPES.contains(qualifiedName(type))
        || (type instanceof DeclaredType declaredType
            && declaredType.asElement().getKind() == ElementKind.ENUM);
  }

  private static String publicName(VariableElement field) {
    String name = field.getAnnotation(FreezerplatePublic.class).name();
    return name.isEmpty() ? field.getSimpleName().toString() : name;
  }

  private static ClassName mapperTypeOf(String dtoPackage, TypeElement entity) {
    return ClassName.get(dtoPackage, entity.getSimpleName() + MAPPER_SUFFIX);
  }

  private String packageOf(TypeElement entity) {
    return processingEnvironment.getElementUtils().getPackageOf(entity).getQualifiedName().toString();
  }
}
