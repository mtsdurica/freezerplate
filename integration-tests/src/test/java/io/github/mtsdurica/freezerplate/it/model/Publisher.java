package io.github.mtsdurica.freezerplate.it.model;

import io.github.mtsdurica.freezerplate.annotation.FreezerplatePublic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Publisher {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @FreezerplatePublic
  private Integer id;

  @FreezerplatePublic private String name;

  private String taxNumber;

  @FreezerplatePublic
  @OneToMany(mappedBy = "publisher")
  private Set<Magazine> magazines = new HashSet<>();

  protected Publisher() {}

  public Publisher(String name, String taxNumber) {
    this.name = name;
    this.taxNumber = taxNumber;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTaxNumber() {
    return taxNumber;
  }

  public void setTaxNumber(String taxNumber) {
    this.taxNumber = taxNumber;
  }

  public Set<Magazine> getMagazines() {
    return magazines;
  }

  public void setMagazines(Set<Magazine> magazines) {
    this.magazines = magazines;
  }
}
