package io.github.mtsdurica.freezerplate.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mtsdurica.freezerplate.it.model.Magazine;
import io.github.mtsdurica.freezerplate.it.repository.MagazineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestDtoApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired PressFixture press;
  @Autowired MagazineRepository magazineRepository;

  @BeforeEach
  void seed() {
    press.reset();
  }

  @Test
  void createMagazine_whenRequestIsValid_thenMapsItToEntity() throws Exception {
    mockMvc
        .perform(
            post("/api/magazines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": "The New Yorker", "editorPhone": "+1-555-0142", "publisherId": %d}
                    """
                        .formatted(press.condeNast().getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.headline").value("The New Yorker"))
        .andExpect(jsonPath("$.data.editorPhone").doesNotExist())
        .andExpect(jsonPath("$.data.publisher.name").value("Condé Nast"));
  }

  @Test
  void createMagazine_whenRequiredFieldIsMissing_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/magazines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"publisherId": %d}
                    """
                        .formatted(press.condeNast().getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createMagazine_whenPublisherDoesNotExist_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/magazines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": "Ghost", "publisherId": -1}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateMagazine_whenRequestIsValid_thenAppliesIt() throws Exception {
    mockMvc
        .perform(
            put("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": "Wired UK", "editorPhone": "+44-20-0000", "publisherId": %d}
                    """
                        .formatted(press.hearst().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.headline").value("Wired UK"))
        .andExpect(jsonPath("$.data.publisher.name").value("Hearst"));
  }

  @Test
  void updateMagazine_whenRequiredFieldIsMissing_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            put("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"editorPhone": "+44-20-0000"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchMagazine_whenFieldsAreOmitted_thenKeepsThem() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": "Wired UK"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.headline").value("Wired UK"))
        .andExpect(jsonPath("$.data.publisher.name").value("Condé Nast"));

    Magazine stored = magazineRepository.findById(press.wired().getId()).orElseThrow();
    assertEquals("+1-555-0100", stored.getEditorPhone());
  }

  @Test
  void patchMagazine_whenPublisherIdIsGiven_thenMovesMagazine() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"publisherId": %d}
                    """
                        .formatted(press.hearst().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.headline").value("Wired"))
        .andExpect(jsonPath("$.data.publisher.name").value("Hearst"));
  }

  @Test
  void patchMagazine_whenFieldIsNull_thenClearsIt() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"editorPhone": null}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.headline").value("Wired"));

    Magazine stored = magazineRepository.findById(press.wired().getId()).orElseThrow();
    assertNull(stored.getEditorPhone());
  }

  @Test
  void patchMagazine_whenPublisherIdIsNull_thenDetachesPublisher() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"publisherId": null}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publisher").doesNotExist());

    Magazine stored = magazineRepository.findById(press.wired().getId()).orElseThrow();
    assertNull(stored.getPublisher());
  }

  @Test
  void patchMagazine_whenResultViolatesConstraints_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": null}
                    """))
        .andExpect(status().isBadRequest());

    Magazine stored = magazineRepository.findById(press.wired().getId()).orElseThrow();
    assertEquals("Wired", stored.getTitle());
  }

  @Test
  void getApiDocs_whenRepositoryHasRequestDto_thenRequestBodiesUseItAndItsPatchVariant() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.paths['/api/magazines'].post.requestBody.content['application/json'].schema['$ref']",
                endsWith("/MagazineRequest")))
        .andExpect(
            jsonPath(
                "$.paths['/api/magazines/{id}'].put.requestBody.content['application/json'].schema['$ref']",
                endsWith("/MagazineRequest")))
        .andExpect(
            jsonPath(
                "$.paths['/api/magazines/{id}'].patch.requestBody.content['application/json'].schema['$ref']",
                endsWith("/MagazineRequestPatch")));
  }
}
