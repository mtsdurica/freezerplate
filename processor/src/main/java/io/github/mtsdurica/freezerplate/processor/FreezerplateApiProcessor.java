package io.github.mtsdurica.freezerplate.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * Annotation processor that generates Freezerplate's controllers, DTOs and mappers at compile
 * time from {@code @FreezerplateApi} repositories and {@code @FreezerplatePublic} entity fields.
 */
// AutoService writes the META-INF/services entry that lets javac discover this processor.
@SupportedAnnotationTypes({
  "io.github.mtsdurica.freezerplate.annotation.FreezerplateApi",
  "io.github.mtsdurica.freezerplate.annotation.FreezerplatePublic"
})
@AutoService(Processor.class)
public final class FreezerplateApiProcessor extends AbstractProcessor {

  private DtoGenerator dtoGenerator;
  private RepositoryCollector repositoryCollector;
  private ControllerGenerator controllerGenerator;
  private AssociationGenerator associationGenerator;

  // The processor only reads annotations and never depends on new language syntax, so it supports
  // whatever Java version the compiler runs; a fixed version would make javac warn on newer ones.
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    var sourceWriter = new SourceWriter(processingEnvironment);
    this.dtoGenerator = new DtoGenerator(processingEnvironment, sourceWriter);
    this.repositoryCollector = new RepositoryCollector(processingEnvironment, sourceWriter);
    this.controllerGenerator = new ControllerGenerator(sourceWriter);
    this.associationGenerator = new AssociationGenerator(processingEnvironment, sourceWriter);
  }

  // DTOs are generated first because controllers need to know which entities have a DTO
  // to decide their response types.
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
    Map<String, ClassName> dtoTypesByEntityName = dtoGenerator.generate(roundEnvironment);
    Map<String, RepositoryInfo> repositoriesByEntityName =
        repositoryCollector.collect(roundEnvironment, dtoTypesByEntityName);
    repositoriesByEntityName.values().forEach(controllerGenerator::generate);
    associationGenerator.generate(repositoriesByEntityName);
    return true;
  }
}
