package io.github.mtsdurica.freezerplate.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

class BasePathTest {

  @Nested
  @SpringBootTest(properties = "freezerplate.base-path=")
  @AutoConfigureMockMvc
  class WithoutBasePath {

    @Autowired MockMvc mockMvc;
    @Autowired LibraryFixture library;

    @BeforeEach
    void seed() {
      library.reset();
    }

    @Test
    void getBooks_whenBasePathIsNotSet_thenServesAtRoot() throws Exception {
      mockMvc
          .perform(get("/books"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items.length()").value(4));
    }

    @Test
    void getBooks_whenBasePathIsNotSet_thenApiPrefixIsNotServed() throws Exception {
      mockMvc.perform(get("/api/books")).andExpect(status().isNotFound());
    }

    @Test
    void getApiDocs_whenBasePathIsNotSet_thenPathsHaveNoPrefix() throws Exception {
      mockMvc
          .perform(get("/v3/api-docs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.paths['/books'].get").exists())
          .andExpect(jsonPath("$.paths['/books/{id}/authors'].get").exists());
    }
  }

  @Nested
  @SpringBootTest(properties = "freezerplate.base-path=v1/")
  @AutoConfigureMockMvc
  class WithUnnormalizedBasePath {

    @Autowired MockMvc mockMvc;
    @Autowired LibraryFixture library;

    @BeforeEach
    void seed() {
      library.reset();
    }

    @Test
    void getBooks_whenBasePathLacksLeadingSlashAndHasTrailingSlash_thenServesNormalizedPath()
        throws Exception {
      mockMvc.perform(get("/v1/books")).andExpect(status().isOk());
    }
  }
}
