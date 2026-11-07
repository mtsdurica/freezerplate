package io.github.mtsdurica.freezerplate.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mtsdurica.freezerplate.it.model.Category;
import io.github.mtsdurica.freezerplate.it.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CustomPathApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired CategoryRepository categoryRepository;

  private Category fantasy;

  @BeforeEach
  void seed() {
    categoryRepository.deleteAll();
    fantasy = categoryRepository.save(new Category("Fantasy"));
  }

  @Test
  void getCategories_whenPathIsConfigured_thenServesItUnderTheBasePath() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Fantasy"));
  }

  @Test
  void getCategories_whenPathIsConfigured_thenDefaultPathIsNotServed() throws Exception {
    mockMvc.perform(get("/api/categorys")).andExpect(status().isNotFound());
  }

  @Test
  void getCategoryBooks_whenOwnerPathIsConfigured_thenAssociationUsesIt() throws Exception {
    mockMvc
        .perform(get("/api/categories/{id}/books", fantasy.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void getApiDocs_whenPathIsConfigured_thenOperationIdUsesItsPlural() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/categories'].get.operationId").value("getCategories"))
        .andExpect(jsonPath("$.paths['/api/categories/{id}'].get.operationId").value("getCategory"))
        .andExpect(jsonPath("$.paths['/api/categories/{id}/books'].get").exists())
        .andExpect(jsonPath("$.paths['/api/categorys']").doesNotExist());
  }
}
