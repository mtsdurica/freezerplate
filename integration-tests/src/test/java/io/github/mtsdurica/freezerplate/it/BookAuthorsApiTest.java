package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookAuthorsApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired LibraryFixture library;

  @BeforeEach
  void seed() {
    library.reset();
  }

  @Test
  void getBookAuthors_whenBookHasAuthors_thenReturnsThem() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}/authors", library.goodOmens().getId()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.items[*].name", containsInAnyOrder("Terry Pratchett", "Neil Gaiman")));
  }

  @Test
  void getBookAuthors_whenBookDoesNotExist_thenReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/books/{id}/authors", -1)).andExpect(status().isNotFound());
  }

  @Test
  void replaceBookAuthors_whenIdsGiven_thenAuthorsAreReplaced() throws Exception {
    mockMvc
        .perform(
            put("/api/books/{id}/authors", library.goodOmens().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[%d]".formatted(library.herbert().getId())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/books/{id}/authors", library.goodOmens().getId()))
        .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Frank Herbert")));
  }

  @Test
  void addBookAuthors_whenIdsGiven_thenAuthorsAreAdded() throws Exception {
    mockMvc
        .perform(
            post("/api/books/{id}/authors", library.dune().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[%d]".formatted(library.gaiman().getId())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/books/{id}/authors", library.dune().getId()))
        .andExpect(
            jsonPath("$.items[*].name", containsInAnyOrder("Frank Herbert", "Neil Gaiman")));
  }

  @Test
  void addBookAuthors_whenAuthorDoesNotExist_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/books/{id}/authors", library.dune().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[-1]"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeBookAuthor_whenAuthorIsLinked_thenAuthorIsRemoved() throws Exception {
    mockMvc
        .perform(
            delete(
                "/api/books/{id}/authors/{authorId}",
                library.goodOmens().getId(),
                library.gaiman().getId()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/books/{id}/authors", library.goodOmens().getId()))
        .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Terry Pratchett")));
  }
}
