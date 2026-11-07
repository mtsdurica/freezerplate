package io.github.mtsdurica.freezerplate.autoconfigure;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;

/**
 * Builds the {@code errors} extension of problem responses, converting property paths and Jackson
 * paths into JSON Pointers (RFC 6901) that identify the offending request field.
 */
final class ProblemErrors {

  static final String PROPERTY = "errors";

  private ProblemErrors() {}

  static Map<String, String> error(String pointer, String detail) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("pointer", pointer);
    error.put("detail", detail);
    return error;
  }

  static String pointerFromPropertyPath(String propertyPath) {
    List<String> segments =
        Arrays.stream(propertyPath.replace("[", ".").replace("]", "").split("\\."))
            .filter(segment -> !segment.isEmpty())
            .toList();
    return toPointer(segments);
  }

  static String pointerFromJsonPath(List<JacksonException.Reference> path) {
    List<String> segments =
        path.stream()
            .map(reference ->
                reference.getPropertyName() != null
                    ? reference.getPropertyName()
                    : String.valueOf(reference.getIndex()))
            .toList();
    return toPointer(segments);
  }

  private static String toPointer(List<String> segments) {
    return "#"
        + segments.stream()
            .map(segment -> "/" + segment.replace("~", "~0").replace("/", "~1"))
            .collect(Collectors.joining());
  }
}
