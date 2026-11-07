package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi
public interface BookRepository extends JpaRepository<Book, Integer> {}
