package io.github.mtsdurica.freezerplate.it.repository;

import io.github.mtsdurica.freezerplate.annotation.FreezerplateApi;
import io.github.mtsdurica.freezerplate.it.api.MagazineRequest;
import io.github.mtsdurica.freezerplate.it.model.Magazine;
import org.springframework.data.jpa.repository.JpaRepository;

@FreezerplateApi(request = MagazineRequest.class)
public interface MagazineRepository extends JpaRepository<Magazine, Integer> {}
