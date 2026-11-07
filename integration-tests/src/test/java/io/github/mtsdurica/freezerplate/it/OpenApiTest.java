package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

  @Autowired MockMvc mockMvc;

  @Test
  void getApiDocs_whenRepositoryHasFreezerplateApi_thenSpecContainsItsEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/books'].get").exists())
        .andExpect(jsonPath("$.paths['/api/books'].post").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].get").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].put").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].patch").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].delete").exists());
  }

  @Test
  void getApiDocs_whenRepositoryHasFreezerplateApi_thenOperationIdsNameTheEntity()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/books'].get.operationId").value("getBooks"))
        .andExpect(jsonPath("$.paths['/api/books'].post.operationId").value("createBook"))
        .andExpect(jsonPath("$.paths['/api/books/{id}'].get.operationId").value("getBook"))
        .andExpect(jsonPath("$.paths['/api/books/{id}'].put.operationId").value("updateBook"))
        .andExpect(jsonPath("$.paths['/api/books/{id}'].patch.operationId").value("patchBook"))
        .andExpect(jsonPath("$.paths['/api/books/{id}'].delete.operationId").value("deleteBook"));
  }

  @Test
  void getApiDocs_whenEndpointsRespond_thenResponseSchemasUseResponseWrapperNames()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/books'].get.responses['200'].content['application/json'].schema['$ref']",
                endsWith("/CollectionResponseBook")))
        .andExpect(
            jsonPath("$.paths['/api/books/{id}'].get.responses['200'].content['application/json'].schema['$ref']",
                endsWith("/ResourceResponseBook")))
        .andExpect(jsonPath("$.components.schemas.EntityModelBook").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.CollectionModelBook").doesNotExist());
  }

  @Test
  void getApiDocs_whenEntityOwnsCollectionRelation_thenSpecContainsAssociationEndpoints()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/books/{id}/authors'].get.operationId").value("getBookAuthors"))
        .andExpect(
            jsonPath("$.paths['/api/books/{id}/authors'].put.operationId")
                .value("replaceBookAuthors"))
        .andExpect(
            jsonPath("$.paths['/api/books/{id}/authors'].post.operationId").value("addBookAuthors"))
        .andExpect(
            jsonPath("$.paths['/api/books/{id}/authors/{targetId}'].delete.operationId")
                .value("removeBookAuthors"));
  }

  @Test
  void getApiDocs_whenOperationReadsOneEntity_thenDocumentsNotFoundProblem() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.paths['/api/books/{id}'].get.responses['404'].content['application/problem+json'].schema['$ref']",
                endsWith("/ProblemDetail")))
        .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['409']").doesNotExist());
  }

  @Test
  void getApiDocs_whenOperationListsEntities_thenDocumentsOnlyServerError() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/books'].get.responses['500']").exists())
        .andExpect(jsonPath("$.paths['/api/books'].get.responses['400']").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/books'].get.responses['404']").doesNotExist());
  }

  @Test
  void getApiDocs_whenOperationCreatesEntity_thenDocumentsBadRequestAndConflict()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/books'].post.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/books'].post.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/books'].post.responses['500']").exists())
        .andExpect(jsonPath("$.paths['/api/books'].post.responses['404']").doesNotExist());
  }

  @Test
  void getApiDocs_whenOperationChangesExistingEntity_thenDocumentsAllClientErrors()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].patch.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].patch.responses['404']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].patch.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].delete.responses['404']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}'].delete.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}/authors'].post.responses['404']").exists())
        .andExpect(jsonPath("$.paths['/api/books/{id}/authors'].post.responses['400']").exists());
  }

  @Test
  void getApiDocs_whenOperationSucceeds_thenResponseIsDocumentedAsJson() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/books/{id}'].get.responses['200'].content['application/json']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/books/{id}'].get.responses['200'].content['*/*']")
                .doesNotExist());
  }

  @Test
  void getApiDocs_whenErrorsAreDocumented_thenProblemSchemaHasRfc9457Members() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.type").exists())
        .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.title").exists())
        .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.status").exists())
        .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.detail").exists())
        .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.instance").exists())
        .andExpect(
            jsonPath("$.components.schemas.ProblemDetail.properties.errors.items.properties.pointer")
                .exists());
  }

  @Test
  void getApiDocs_whenEntityIsRequestBody_thenRelationsAreDocumentedAsIds() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.paths['/api/books'].post.requestBody.content['application/json'].schema['$ref']",
                endsWith("/BookRequest")))
        .andExpect(
            jsonPath(
                "$.paths['/api/books/{id}'].put.requestBody.content['application/json'].schema['$ref']",
                endsWith("/BookRequest")))
        .andExpect(jsonPath("$.components.schemas.BookRequest.properties.title.type").value("string"))
        .andExpect(
            jsonPath("$.components.schemas.BookRequest.properties.authors.type").value("array"))
        .andExpect(
            jsonPath("$.components.schemas.BookRequest.properties.authors.items.type")
                .value("integer"))
        .andExpect(
            jsonPath("$.components.schemas.ReviewRequest.properties.book.type").value("integer"));
  }

  @Test
  void getApiDocs_whenEntityIsRequestBody_thenIdAndInverseRelationsAreOmitted() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.BookRequest.properties.id").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.BookRequest.properties.reviews").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.AuthorRequest.properties.books").doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.PublisherRequest.properties.magazines").doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.PublisherRequest.properties.taxNumber").exists());
  }

  @Test
  void getApiDocs_whenEntityIsPatched_thenPatchSchemaIsOptionalAndNullable() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.paths['/api/books/{id}'].patch.requestBody.content['application/json'].schema['$ref']",
                endsWith("/BookRequestPatch")))
        .andExpect(jsonPath("$.components.schemas.BookRequestPatch.required").doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.BookRequestPatch.properties.authors.anyOf[0].type")
                .value("array"))
        .andExpect(
            jsonPath("$.components.schemas.BookRequestPatch.properties.authors.anyOf[1].type")
                .value("null"));
  }

  @Test
  void getApiDocs_whenRequestDtoIsPatched_thenPatchSchemaDropsRequiredFields() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.MagazineRequest.required[0]").value("headline"))
        .andExpect(jsonPath("$.components.schemas.MagazineRequestPatch.required").doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.MagazineRequestPatch.properties.headline.anyOf[1].type")
                .value("null"));
  }

  @Test
  void getApiDocs_whenEntityIsOnlyUsedAsRequestBody_thenEntitySchemaIsNotPublished()
      throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.Publisher").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.Magazine").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.Book").exists());
  }
}
