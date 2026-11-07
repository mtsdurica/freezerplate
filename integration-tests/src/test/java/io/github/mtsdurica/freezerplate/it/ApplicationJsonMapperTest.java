package io.github.mtsdurica.freezerplate.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mtsdurica.freezerplate.it.model.Author;
import io.github.mtsdurica.freezerplate.it.model.Book;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class ApplicationJsonMapperTest {

  @Autowired JsonMapper jsonMapper;

  @Test
  void readValue_whenEntityHasRelations_thenRelationsAreDeserialized() {
    Book book =
        jsonMapper.readValue(
            """
            {"title": "Dune", "authors": [{"name": "Frank Herbert"}]}
            """,
            Book.class);

    assertEquals(List.of("Frank Herbert"), book.getAuthors().stream().map(Author::getName).toList());
  }

  @Test
  void writeValueAsString_whenEntityHasRelations_thenReverseSideIsSerialized() {
    Author herbert = new Author("Frank Herbert");
    herbert.setBooks(Set.of(new Book("Dune", Set.of())));

    String json = jsonMapper.writeValueAsString(herbert);

    assertTrue(json.contains("\"authors\":[]"), json);
  }
}
