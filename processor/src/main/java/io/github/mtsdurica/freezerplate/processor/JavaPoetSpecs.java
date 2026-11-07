package io.github.mtsdurica.freezerplate.processor;

import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.HTTP_STATUS;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.PATH_VARIABLE;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.REQUEST_BODY;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.RESPONSE_STATUS;
import static io.github.mtsdurica.freezerplate.processor.GeneratedTypes.VALID;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;

final class JavaPoetSpecs {

  static final String ID = "id";
  static final String ID_PATH = "/{id}";

  private static final String VALUE = "value";
  private static final String MAP_TO_RESPONSE_DTO = ".map(responseMapper::toDto)";

  private JavaPoetSpecs() {}

  static AnnotationSpec annotationWithValue(ClassName annotation, String value) {
    return AnnotationSpec.builder(annotation).addMember(VALUE, "$S", value).build();
  }

  static AnnotationSpec annotationWithClass(ClassName annotation, TypeName type) {
    return AnnotationSpec.builder(annotation).addMember(VALUE, "$T.class", type).build();
  }

  static AnnotationSpec responseStatus(String httpStatus) {
    return AnnotationSpec.builder(RESPONSE_STATUS)
        .addMember(VALUE, "$T.$L", HTTP_STATUS, httpStatus)
        .build();
  }

  static ParameterSpec pathVariable(TypeName type, String name) {
    return ParameterSpec.builder(type, name).addAnnotation(PATH_VARIABLE).build();
  }

  static ParameterSpec requestBody(TypeName type, String name) {
    return ParameterSpec.builder(type, name).addAnnotation(REQUEST_BODY).build();
  }

  static ParameterSpec validatedRequestBody(TypeName type, String name) {
    return ParameterSpec.builder(type, name).addAnnotation(VALID).addAnnotation(REQUEST_BODY).build();
  }

  static FieldSpec privateFinalField(TypeName type, String name) {
    return FieldSpec.builder(type, name, Modifier.PRIVATE, Modifier.FINAL).build();
  }

  static String mapToResponse(boolean hasResponseDto) {
    return hasResponseDto ? MAP_TO_RESPONSE_DTO : "";
  }
}
