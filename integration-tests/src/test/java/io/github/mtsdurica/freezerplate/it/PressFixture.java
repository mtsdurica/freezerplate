package io.github.mtsdurica.freezerplate.it;

import io.github.mtsdurica.freezerplate.it.model.Magazine;
import io.github.mtsdurica.freezerplate.it.model.Publisher;
import io.github.mtsdurica.freezerplate.it.repository.MagazineRepository;
import io.github.mtsdurica.freezerplate.it.repository.PublisherRepository;
import org.springframework.stereotype.Component;

@Component
public class PressFixture {

  private final PublisherRepository publisherRepository;
  private final MagazineRepository magazineRepository;

  private Publisher condeNast;
  private Publisher hearst;
  private Magazine wired;
  private Magazine vogue;

  public PressFixture(
      PublisherRepository publisherRepository, MagazineRepository magazineRepository) {
    this.publisherRepository = publisherRepository;
    this.magazineRepository = magazineRepository;
  }

  public void reset() {
    magazineRepository.deleteAll();
    publisherRepository.deleteAll();
    condeNast = publisherRepository.save(new Publisher("Condé Nast", "US-123456"));
    hearst = publisherRepository.save(new Publisher("Hearst", "US-654321"));
    wired = magazineRepository.save(new Magazine("Wired", "+1-555-0100", condeNast));
    vogue = magazineRepository.save(new Magazine("Vogue", "+1-555-0199", condeNast));
  }

  public Publisher condeNast() {
    return condeNast;
  }

  public Publisher hearst() {
    return hearst;
  }

  public Magazine wired() {
    return wired;
  }

  public Magazine vogue() {
    return vogue;
  }
}
