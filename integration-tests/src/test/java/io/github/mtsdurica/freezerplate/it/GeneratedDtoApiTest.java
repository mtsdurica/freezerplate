package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GeneratedDtoApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired PressFixture press;

  @BeforeEach
  void seed() {
    press.reset();
  }

  @Test
  void getPublisherById_whenFieldIsNotPublic_thenOmitsIt() throws Exception {
    mockMvc
        .perform(get("/api/publishers/{id}", press.condeNast().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(press.condeNast().getId()))
        .andExpect(jsonPath("$.data.name").value("Condé Nast"))
        .andExpect(jsonPath("$.data.taxNumber").doesNotExist());
  }

  @Test
  void getPublisherById_whenPublisherHasMagazines_thenNestedMagazinesOmitTheirPublisher()
      throws Exception {
    mockMvc
        .perform(get("/api/publishers/{id}", press.condeNast().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.magazines[*].headline", containsInAnyOrder("Wired", "Vogue")))
        .andExpect(jsonPath("$.data.magazines[*].editorPhone").doesNotExist())
        .andExpect(jsonPath("$.data.magazines[*].publisher").doesNotExist());
  }

  @Test
  void getMagazineById_whenFieldIsRenamed_thenUsesPublicName() throws Exception {
    mockMvc
        .perform(get("/api/magazines/{id}", press.wired().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.headline").value("Wired"))
        .andExpect(jsonPath("$.data.title").doesNotExist())
        .andExpect(jsonPath("$.data.editorPhone").doesNotExist());
  }

  @Test
  void getMagazineById_whenMagazineHasPublisher_thenNestedPublisherOmitsItsMagazines()
      throws Exception {
    mockMvc
        .perform(get("/api/magazines/{id}", press.wired().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publisher.name").value("Condé Nast"))
        .andExpect(jsonPath("$.data.publisher.taxNumber").doesNotExist())
        .andExpect(jsonPath("$.data.publisher.magazines").doesNotExist());
  }

  @Test
  void getMagazines_whenMagazinesExist_thenReturnsThemAsDtos() throws Exception {
    mockMvc
        .perform(get("/api/magazines"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].headline", containsInAnyOrder("Wired", "Vogue")))
        .andExpect(jsonPath("$.items[*].editorPhone").doesNotExist());
  }
}
