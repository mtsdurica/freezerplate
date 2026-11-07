package io.github.mtsdurica.freezerplate.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

final class SourceWriter {

  private final ProcessingEnvironment processingEnvironment;

  SourceWriter(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  // A hand-written class with the same name replaces the generated one; this is how users
  // override a single controller, DTO or mapper without disabling the processor.
  boolean existsAlready(ClassName type) {
    if (processingEnvironment.getElementUtils().getTypeElement(type.canonicalName()) == null) {
      return false;
    }
    note(type.canonicalName() + " already exists - using your version, skipping generation.");
    return true;
  }

  void write(ClassName type, TypeSpec typeSpec, Element origin) {
    try {
      JavaFile.builder(type.packageName(), typeSpec)
          .build()
          .writeTo(processingEnvironment.getFiler());
      note("freezerplate: generated " + type.canonicalName());
    } catch (IOException exception) {
      error("Failed to write " + type.canonicalName() + ": " + exception.getMessage(), origin);
    }
  }

  void error(String message, Element element) {
    processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  private void note(String message) {
    processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
  }
}
