package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi(path = "/categories/")
public interface CategoryRepository extends JpaRepository<Category, Integer> {}
