package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi
public interface ReviewRepository extends JpaRepository<Review, Integer> {}