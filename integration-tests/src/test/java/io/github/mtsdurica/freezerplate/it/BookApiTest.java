package io.github.mtsdurica.freezerplate.it;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class BookApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired LibraryFixture library;

  @BeforeEach
  void seed() {
    library.reset();
  }

  @Test
  void getBooks_whenBooksExist_thenReturnsAllBooks() throws Exception {
    mockMvc
        .perform(get("/api/books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(4))
        .andExpect(
            jsonPath(
                "$.items[*].title",
                containsInAnyOrder("Dune", "The Hobbit", "The Lord of the Rings", "Good Omens")));
  }

  @Test
  void getBookById_whenAuthorWroteSeveralBooks_thenEachBookReturnsSameAuthor() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.hobbit().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.authors[*].id", contains(library.tolkien().getId())));

    mockMvc
        .perform(get("/api/books/{id}", library.lordOfTheRings().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.authors[*].id", contains(library.tolkien().getId())));
  }

  @Test
  void getBookById_whenBookHasReviews_thenReturnsItsReviews() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.hobbit().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.length()").value(2))
        .andExpect(
            jsonPath(
                "$.data.reviews[*].text",
                containsInAnyOrder("A timeless adventure", "Charming, a bit slow in the middle")));
  }

  @Test
  void getBookById_whenBookHasNoReviews_thenReturnsEmptyReviews() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.dune().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews").isEmpty());
  }

  @Test
  void getBookById_whenBookExists_thenReturnsThatBook() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.dune().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(library.dune().getId()))
        .andExpect(jsonPath("$.data.title").value("Dune"))
        .andExpect(jsonPath("$.data.authors[*].name", containsInAnyOrder("Frank Herbert")));
  }

  @Test
  void getBookById_whenBookHasSeveralAuthors_thenReturnsAllAuthors() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.goodOmens().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Good Omens"))
        .andExpect(jsonPath("$.data.authors.length()").value(2))
        .andExpect(
            jsonPath(
                "$.data.authors[*].name", containsInAnyOrder("Terry Pratchett", "Neil Gaiman")));
  }

  @Test
  void getBookById_whenBookHasAuthors_thenNestedAuthorsOmitTheirBooks() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.goodOmens().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.authors.length()").value(2))
        .andExpect(jsonPath("$.data.authors[*].books").doesNotExist());
  }

  @Test
  void getBookById_whenBookHasReviews_thenNestedReviewsOmitTheirBook() throws Exception {
    mockMvc
        .perform(get("/api/books/{id}", library.hobbit().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.length()").value(2))
        .andExpect(jsonPath("$.data.reviews[*].book").doesNotExist());
  }

  @Test
  void getBookById_whenBookDoesNotExist_thenReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/books/{id}", -1)).andExpect(status().isNotFound());
  }

  @Test
  void createBook_whenBodyReferencesAuthorIds_thenBookIsLinkedToThoseAuthors() throws Exception {
    mockMvc
        .perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Neverwhere", "authors": [%d, %d]}
                    """
                        .formatted(library.gaiman().getId(), library.pratchett().getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.title").value("Neverwhere"))
        .andExpect(
            jsonPath("$.data.authors[*].name", containsInAnyOrder("Neil Gaiman", "Terry Pratchett")));
  }

  @Test
  void createBook_whenBodyOmitsAuthors_thenBookHasNoAuthors() throws Exception {
    mockMvc
        .perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Anonymous"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.authors.length()").value(0));
  }

  @Test
  void createBook_whenBodyReferencesUnknownAuthor_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Ghostwritten", "authors": [-1]}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateBook_whenBodyOmitsAuthors_thenAuthorsAreKept() throws Exception {
    mockMvc
        .perform(
            put("/api/books/{id}", library.hobbit().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "There and Back Again"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("There and Back Again"))
        .andExpect(jsonPath("$.data.authors[*].name", contains("J.R.R. Tolkien")));
  }

  @Test
  void updateBook_whenBodyContainsAuthorIds_thenAuthorsAreKept() throws Exception {
    mockMvc
        .perform(
            put("/api/books/{id}", library.hobbit().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "The Hobbit", "authors": [%d]}
                    """
                        .formatted(library.herbert().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.authors[*].name", contains("J.R.R. Tolkien")));
  }

  @Test
  void patchBook_whenBodyOmitsAuthors_thenAuthorsAreUnchanged() throws Exception {
    mockMvc
        .perform(
            patch("/api/books/{id}", library.hobbit().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "There and Back Again"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("There and Back Again"))
        .andExpect(jsonPath("$.data.authors[*].name", contains("J.R.R. Tolkien")));
  }

  @Test
  void patchBook_whenBodyContainsAuthorIds_thenAuthorsAreReplaced() throws Exception {
    mockMvc
        .perform(
            patch("/api/books/{id}", library.hobbit().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"authors": [%d]}
                    """
                        .formatted(library.herbert().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("The Hobbit"))
        .andExpect(jsonPath("$.data.authors[*].name", contains("Frank Herbert")));
  }
}
