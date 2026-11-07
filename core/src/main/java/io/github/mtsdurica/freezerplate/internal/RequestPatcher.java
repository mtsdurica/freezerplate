package io.github.mtsdurica.freezerplate.internal;

import io.github.mtsdurica.freezerplate.web.FreezerplateRequestMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Applies a JSON Merge Patch (RFC 7396) in the vocabulary of the request DTO: the stored entity
 * is turned into a complete DTO, the patch is merged on top and the result is validated before
 * the mapper applies it. Merging works on the JSON tree because DTOs may be immutable.
 */
public final class RequestPatcher {

  private final JsonMapper mapper;
  private final Validator validator;

  public RequestPatcher(FreezerplateJsonMapper jsonMapper, Validator validator) {
    this.mapper = jsonMapper.mapper();
    this.validator = validator;
  }

  public <R, T> void apply(
      T entity, JsonNode patch, FreezerplateRequestMapper<R, T> requestMapper) {
    R patched = merge(requestMapper.toRequest(entity), patch);
    validate(patched);
    requestMapper.update(patched, entity);
  }

  @SuppressWarnings("unchecked")
  private <R> R merge(R current, JsonNode patch) {
    JsonNode merged = mergePatch(mapper.valueToTree(current), patch);
    return mapper.treeToValue(merged, (Class<R>) current.getClass());
  }

  private JsonNode mergePatch(JsonNode target, JsonNode patch) {
    if (!patch.isObject()) {
      return patch;
    }
    ObjectNode result =
        target.isObject() ? (ObjectNode) target.deepCopy() : mapper.createObjectNode();
    for (Map.Entry<String, JsonNode> property : patch.properties()) {
      if (property.getValue().isNull()) {
        result.remove(property.getKey());
      } else {
        result.set(
            property.getKey(), mergePatch(result.path(property.getKey()), property.getValue()));
      }
    }
    return result;
  }

  private <R> void validate(R request) {
    Set<ConstraintViolation<R>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
