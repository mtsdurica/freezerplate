package io.github.mtsdurica.freezerplate.it.api;

import io.github.mtsdurica.freezerplate.it.model.Magazine;
import io.github.mtsdurica.freezerplate.it.model.Publisher;
import io.github.mtsdurica.freezerplate.it.repository.PublisherRepository;
import io.github.mtsdurica.freezerplate.web.FreezerplateRequestMapper;
import io.github.mtsdurica.freezerplate.web.UnknownReferenceException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MagazineRequestMapper implements FreezerplateRequestMapper<MagazineRequest, Magazine> {

  private final PublisherRepository publisherRepository;

  public MagazineRequestMapper(PublisherRepository publisherRepository) {
    this.publisherRepository = publisherRepository;
  }

  @Override
  public Magazine toEntity(MagazineRequest request) {
    return new Magazine(
        request.getHeadline(),
        request.getEditorPhone(),
        publisherOf(request.getPublisherId()).orElse(null));
  }

  @Override
  public MagazineRequest toRequest(Magazine magazine) {
    return new MagazineRequest(
        magazine.getTitle(),
        magazine.getEditorPhone(),
        Optional.ofNullable(magazine.getPublisher()).map(Publisher::getId).orElse(null));
  }

  @Override
  public void update(MagazineRequest request, Magazine magazine) {
    magazine.setTitle(request.getHeadline());
    magazine.setEditorPhone(request.getEditorPhone());
    magazine.setPublisher(publisherOf(request.getPublisherId()).orElse(null));
  }

  private Optional<Publisher> publisherOf(Integer publisherId) {
    return Optional.ofNullable(publisherId)
        .map(
            id ->
                publisherRepository
                    .findById(id)
                    .orElseThrow(() -> new UnknownReferenceException(Publisher.class, id)));
  }
}
