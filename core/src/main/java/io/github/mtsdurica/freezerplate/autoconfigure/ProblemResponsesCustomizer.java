package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

/**
 * springdoc cannot infer error responses because FreezerplateExceptionHandler chooses statuses at
 * runtime, so each Freezerplate operation is documented with the errors it can actually return.
 */
final class ProblemResponsesCustomizer implements OperationCustomizer, OpenApiCustomizer {

  private static final String PROBLEM_SCHEMA = "ProblemDetail";
  private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String ANY_MEDIA_TYPE = "*/*";
  private static final Set<RequestMethod> MUTATING_METHODS =
      EnumSet.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    if (!handlerMethod.getBeanType().isAnnotationPresent(FreezerplateFor.class)) {
      return operation;
    }
    ApiResponses responses =
        Optional.ofNullable(operation.getResponses()).orElseGet(ApiResponses::new);
    operation.setResponses(responses);

    declareJsonContent(responses);
    boolean addressesExistingEntity = hasPathParameters(operation);
    if (operation.getRequestBody() != null || addressesExistingEntity) {
      addProblem(responses, "400", "The request is invalid");
    }
    if (addressesExistingEntity) {
      addProblem(responses, "404", "The entity was not found");
    }
    if (isMutating(handlerMethod)) {
      addProblem(responses, "409", "The request conflicts with the current state of the data");
    }
    addProblem(responses, "500", "An unexpected error occurred");
    return operation;
  }

  @Override
  public void customise(OpenAPI openApi) {
    Components components = Optional.ofNullable(openApi.getComponents()).orElseGet(Components::new);
    openApi.setComponents(components);
    components.addSchemas(PROBLEM_SCHEMA, problemSchema());
  }

  private static void declareJsonContent(ApiResponses responses) {
    for (ApiResponse response : responses.values()) {
      Content content = response.getContent();
      if (content != null && content.containsKey(ANY_MEDIA_TYPE)) {
        content.addMediaType(JSON_MEDIA_TYPE, content.remove(ANY_MEDIA_TYPE));
      }
    }
  }

  private static boolean hasPathParameters(Operation operation) {
    List<Parameter> parameters = operation.getParameters();
    return parameters != null
        && parameters.stream().anyMatch(parameter -> "path".equals(parameter.getIn()));
  }

  private static boolean isMutating(HandlerMethod handlerMethod) {
    RequestMapping mapping =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class);
    return mapping != null && Arrays.stream(mapping.method()).anyMatch(MUTATING_METHODS::contains);
  }

  private static void addProblem(ApiResponses responses, String status, String description) {
    if (responses.containsKey(status)) {
      return;
    }
    responses.addApiResponse(
        status,
        new ApiResponse()
            .description(description)
            .content(
                new Content()
                    .addMediaType(
                        PROBLEM_MEDIA_TYPE,
                        new MediaType().schema(new Schema<>().$ref(PROBLEM_SCHEMA)))));
  }

  private static Schema<?> problemSchema() {
    Schema<?> fieldError =
        new ObjectSchema()
            .addProperty(
                "pointer", new StringSchema().description("JSON Pointer to the offending field"))
            .addProperty("detail", new StringSchema().description("What is wrong with the field"));
    return new ObjectSchema()
        .description("RFC 9457 Problem Details")
        .addProperty(
            "type",
            new StringSchema().format("uri").description("Problem type; absent means about:blank"))
        .addProperty("title", new StringSchema().description("Short summary of the problem"))
        .addProperty("status", new IntegerSchema().description("HTTP status code"))
        .addProperty("detail", new StringSchema().description("Explanation of this occurrence"))
        .addProperty(
            "instance", new StringSchema().format("uri").description("Request path that failed"))
        .addProperty(
            "errors",
            new ArraySchema()
                .items(fieldError)
                .description("Field-level problems, present for invalid request content"));
  }
}
