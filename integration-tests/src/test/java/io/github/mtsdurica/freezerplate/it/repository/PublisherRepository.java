package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.model.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {}
