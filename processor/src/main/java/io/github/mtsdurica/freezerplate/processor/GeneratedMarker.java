package io.github.mtsdurica.freezerplate.processor;

import com.squareup.javapoet.AnnotationSpec;

final class GeneratedMarker {

  private GeneratedMarker() {}

  static AnnotationSpec annotation() {
    return JavaPoetSpecs.annotationWithValue(
        GeneratedTypes.GENERATED, FreezerplateApiProcessor.class.getName());
  }
}
