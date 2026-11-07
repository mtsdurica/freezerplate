package io.github.mtsdurica.freezerplate.autoconfigure;

import io.github.mtsdurica.freezerplate.internal.FreezerplateFor;
import io.github.mtsdurica.freezerplate.web.EntityNotFoundException;
import io.github.mtsdurica.freezerplate.web.UnknownReferenceException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;

/**
 * Renders every error from Freezerplate controllers as RFC 9457 Problem Details. It extends
 * ResponseEntityExceptionHandler to cover Spring MVC's own exceptions, and is scoped to
 * @FreezerplateFor so hand-written controllers keep the application's error handling.
 * It is registered without an explicit order, so application-defined advice runs first.
 */
@RestControllerAdvice(annotations = FreezerplateFor.class)
final class FreezerplateExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(FreezerplateExceptionHandler.class);
  private static final String INVALID_REQUEST_CONTENT = "Invalid request content";

  @ExceptionHandler(EntityNotFoundException.class)
  ResponseEntity<Object> handleEntityNotFound(
      EntityNotFoundException exception, WebRequest request) {
    return problem(exception, HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(UnknownReferenceException.class)
  ResponseEntity<Object> handleUnknownReference(
      UnknownReferenceException exception, WebRequest request) {
    return problem(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Object> handleConstraintViolation(
      ConstraintViolationException exception, WebRequest request) {
    List<Map<String, String>> errors =
        exception.getConstraintViolations().stream()
            .map(violation ->
                ProblemErrors.error(
                    ProblemErrors.pointerFromPropertyPath(violation.getPropertyPath().toString()),
                    violation.getMessage()))
            .toList();
    return invalidContent(exception, errors, request);
  }

  // The database message is not returned because it can reveal table and constraint names.
  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<Object> handleConflict(RuntimeException exception, WebRequest request) {
    String detail =
        exception instanceof OptimisticLockingFailureException
            ? "The resource was modified concurrently"
            : "The request violates a data integrity constraint";
    return problem(exception, HttpStatus.CONFLICT, detail, request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
    LOGGER.error("Unexpected error in Freezerplate endpoint", exception);
    return problem(
        exception, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError ->
                ProblemErrors.error(
                    ProblemErrors.pointerFromPropertyPath(fieldError.getField()),
                    fieldError.getDefaultMessage()))
            .toList();
    return invalidContent(exception, errors, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (!(exception.getCause() instanceof JacksonException jacksonException)) {
      return problem(
          exception, HttpStatus.BAD_REQUEST, "Required request body is missing", request);
    }
    if (!(jacksonException instanceof DatabindException)) {
      return problem(exception, HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }
    String detail =
        findCause(jacksonException, UnknownReferenceException.class)
            .map(Throwable::getMessage)
            .orElse("Invalid value");
    Map<String, String> error =
        ProblemErrors.error(ProblemErrors.pointerFromJsonPath(jacksonException.getPath()), detail);
    return invalidContent(exception, List.of(error), request);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problemDetail
        && problemDetail.getInstance() == null
        && request instanceof ServletWebRequest servletRequest) {
      problemDetail.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

  private ResponseEntity<Object> invalidContent(
      Exception exception, List<Map<String, String>> errors, WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_REQUEST_CONTENT);
    problemDetail.setProperty(ProblemErrors.PROPERTY, errors);
    return handleExceptionInternal(
        exception, problemDetail, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  private ResponseEntity<Object> problem(
      Exception exception, HttpStatus status, String detail, WebRequest request) {
    return handleExceptionInternal(
        exception,
        ProblemDetail.forStatusAndDetail(status, detail),
        new HttpHeaders(),
        status,
        request);
  }

  private static <T extends Throwable> Optional<T> findCause(Throwable throwable, Class<T> type) {
    for (Throwable current = throwable; current != null; current = current.getCause()) {
      if (type.isInstance(current)) {
        return Optional.of(type.cast(current));
      }
    }
    return Optional.empty();
  }
}
