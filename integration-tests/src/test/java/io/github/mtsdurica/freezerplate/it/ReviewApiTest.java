package io.github.mtsdurica.freezerplate.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ReviewApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired LibraryFixture library;

  @BeforeEach
  void seed() {
    library.reset();
  }

  @Test
  void getReviews_whenReviewsExist_thenReturnsAllReviews() throws Exception {
    mockMvc
        .perform(get("/api/reviews"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2));
  }

  @Test
  void getReviewById_whenReviewBelongsToBook_thenReturnsThatBook() throws Exception {
    mockMvc
        .perform(get("/api/reviews/{id}", library.hobbitPraise().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.text").value("A timeless adventure"))
        .andExpect(jsonPath("$.data.book.id").value(library.hobbit().getId()))
        .andExpect(jsonPath("$.data.book.title").value("The Hobbit"));
  }

  @Test
  void getReviewById_whenReviewBelongsToBook_thenNestedBookOmitsItsReviews() throws Exception {
    mockMvc
        .perform(get("/api/reviews/{id}", library.hobbitPraise().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.book.title").value("The Hobbit"))
        .andExpect(jsonPath("$.data.book.reviews").doesNotExist());
  }

  @Test
  void createReview_whenBodyReferencesBookId_thenReviewBelongsToThatBook() throws Exception {
    mockMvc
        .perform(
            post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating": 5, "text": "Spice must flow", "book": %d}
                    """
                        .formatted(library.dune().getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.book.title").value("Dune"));
  }

  @Test
  void createReview_whenBodyReferencesUnknownBook_thenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating": 1, "text": "Lost", "book": -1}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchReview_whenBodyContainsBookId_thenReviewMovesToThatBook() throws Exception {
    mockMvc
        .perform(
            patch("/api/reviews/{id}", library.hobbitPraise().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"book": %d}
                    """
                        .formatted(library.lordOfTheRings().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.text").value("A timeless adventure"))
        .andExpect(jsonPath("$.data.book.title").value("The Lord of the Rings"));
  }
}
