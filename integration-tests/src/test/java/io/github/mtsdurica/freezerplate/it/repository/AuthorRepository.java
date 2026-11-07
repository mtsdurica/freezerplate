package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi
public interface AuthorRepository extends JpaRepository<Author, Integer> {}