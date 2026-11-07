package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthorApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired LibraryFixture library;

  @BeforeEach
  void seed() {
    library.reset();
  }

  @Test
  void getAuthors_whenAuthorsHaveBooks_thenReturnsAllAuthors() throws Exception {
    mockMvc
        .perform(get("/api/authors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(4))
        .andExpect(
            jsonPath(
                "$.items[*].name",
                containsInAnyOrder(
                    "Frank Herbert", "J.R.R. Tolkien", "Terry Pratchett", "Neil Gaiman")));
  }

  @Test
  void getAuthorById_whenAuthorWroteSeveralBooks_thenReturnsAllTheirBooks() throws Exception {
    mockMvc
        .perform(get("/api/authors/{id}", library.tolkien().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("J.R.R. Tolkien"))
        .andExpect(jsonPath("$.data.books.length()").value(2))
        .andExpect(
            jsonPath("$.data.books[*].title", containsInAnyOrder("The Hobbit", "The Lord of the Rings")));
  }

  @Test
  void getAuthorById_whenAuthorCoWroteBook_thenReturnsThatBook() throws Exception {
    mockMvc
        .perform(get("/api/authors/{id}", library.gaiman().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.books[*].title", containsInAnyOrder("Good Omens")));
  }

  @Test
  void getAuthorById_whenAuthorHasBooks_thenNestedBooksOmitTheirAuthors() throws Exception {
    mockMvc
        .perform(get("/api/authors/{id}", library.tolkien().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.books.length()").value(2))
        .andExpect(jsonPath("$.data.books[*].authors").doesNotExist());
  }

  @Test
  void createAuthor_whenBodyContainsBooks_thenBooksAreIgnored() throws Exception {
    mockMvc
        .perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Ursula K. Le Guin", "books": [{"title": "Earthsea"}]}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.name").value("Ursula K. Le Guin"))
        .andExpect(jsonPath("$.data.books.length()").value(0));
  }
}
