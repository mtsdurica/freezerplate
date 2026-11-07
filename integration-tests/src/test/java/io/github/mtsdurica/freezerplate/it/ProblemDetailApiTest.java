package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ProblemDetailApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired LibraryFixture library;
  @Autowired PressFixture press;

  @BeforeEach
  void seed() {
    press.reset();
    library.reset();
  }

  @Test
  void getBookById_whenBookDoesNotExist_thenReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", -1))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").doesNotExist())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.instance").value("/api/books/-1"));
  }

  @Test
  void getBookById_whenIdIsNotANumber_thenReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/api/books/abc"));
  }

  @Test
  void createBook_whenBodyIsMalformed_thenReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Malformed JSON request body"));
  }

  @Test
  void createBook_whenAuthorDoesNotExist_thenProblemPointsToReference() throws Exception {
    mockMvc
        .perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Ghostwritten", "authors": [%d, -1]}
                    """
                        .formatted(library.gaiman().getId())))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].pointer").value("#/authors/1"))
        .andExpect(jsonPath("$.errors[0].detail").value("No Author with id -1"));
  }

  @Test
  void createReview_whenBookDoesNotExist_thenProblemPointsToReference() throws Exception {
    mockMvc
        .perform(
            post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating": 1, "text": "Lost", "book": -1}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].pointer").value("#/book"))
        .andExpect(jsonPath("$.errors[0].detail").value("No Book with id -1"));
  }

  @Test
  void createMagazine_whenRequiredFieldIsMissing_thenProblemListsViolations() throws Exception {
    mockMvc
        .perform(
            post("/api/magazines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"publisherId": %d}
                    """
                        .formatted(press.condeNast().getId())))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].pointer").value("#/headline"))
        .andExpect(jsonPath("$.errors[0].detail").value("must not be blank"));
  }

  @Test
  void patchMagazine_whenResultViolatesConstraints_thenProblemListsViolations() throws Exception {
    mockMvc
        .perform(
            patch("/api/magazines/{id}", press.wired().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"headline": ""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].pointer").value("#/headline"))
        .andExpect(jsonPath("$.errors[0].detail").value("must not be blank"));
  }

  @Test
  void addBookAuthors_whenAuthorDoesNotExist_thenReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/books/{id}/authors", library.dune().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[-1]"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail", containsString("-1")));
  }

  @Test
  void deleteBook_whenReviewsReferenceIt_thenReturnsConflict() throws Exception {
    mockMvc
        .perform(delete("/api/books/{id}", library.hobbit().getId()))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail", containsString("constraint")));
  }
}
