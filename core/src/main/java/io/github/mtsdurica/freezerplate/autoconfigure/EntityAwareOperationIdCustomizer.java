package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.swagger.v3.oas.models.Operation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * Operation ids become method names in generated clients, so they are named after the entity and
 * the action (getBooks, getBook, createBook) instead of springdoc's default, the Java method name,
 * which would be identical (findAll, findOne, ...) for every generated controller. The plural for
 * list operations is taken from the controller's path, which users can set to the correct plural.
 */
final class EntityAwareOperationIdCustomizer implements OperationCustomizer {

  private static final Pattern WORD_SEPARATORS = Pattern.compile("[-_]");

  private static final Map<String, String> ASSOCIATION_VERB_BY_METHOD =
      Map.of(
          "findAll", "get",
          "replace", "replace",
          "add", "add",
          "remove", "remove");

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    FreezerplateFor freezerplateFor =
        handlerMethod.getBeanType().getAnnotation(FreezerplateFor.class);
    if (freezerplateFor == null) {
      return operation;
    }

    String methodName = handlerMethod.getMethod().getName();
    String entityName = freezerplateFor.value().getSimpleName();
    operation.setOperationId(
        freezerplateFor.association().isEmpty()
            ? entityOperationId(methodName, entityName, handlerMethod.getBeanType())
            : associationOperationId(methodName, entityName, freezerplateFor.association()));
    return operation;
  }

  private static String entityOperationId(
      String methodName, String entityName, Class<?> controller) {
    return switch (methodName) {
      case "findAll" -> "get" + pluralName(controller, entityName);
      case "findOne" -> "get" + entityName;
      default -> methodName + entityName;
    };
  }

  private static String associationOperationId(
      String methodName, String entityName, String association) {
    return ASSOCIATION_VERB_BY_METHOD.getOrDefault(methodName, methodName)
        + entityName
        + capitalize(association);
  }

  private static String pluralName(Class<?> controller, String entityName) {
    return Optional.ofNullable(
            AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class))
        .map(RequestMapping::path)
        .filter(paths -> paths.length > 0)
        .map(paths -> paths[0].substring(paths[0].lastIndexOf('/') + 1))
        .filter(lastSegment -> !lastSegment.isEmpty())
        .map(EntityAwareOperationIdCustomizer::toPascalCase)
        .orElse(entityName);
  }

  private static String toPascalCase(String pathSegment) {
    return Arrays.stream(WORD_SEPARATORS.split(pathSegment))
        .filter(word -> !word.isEmpty())
        .map(EntityAwareOperationIdCustomizer::capitalize)
        .collect(Collectors.joining());
  }

  private static String capitalize(String word) {
    return Character.toUpperCase(word.charAt(0)) + word.substring(1);
  }
}
