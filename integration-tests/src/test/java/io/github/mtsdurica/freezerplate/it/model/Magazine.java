package io.github.mtsdurica.freezerplate.it.model;

import io.github.mtsdurica.freezerplate.annotation.FreezerplatePublic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Magazine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @FreezerplatePublic
  private Integer id;

  @FreezerplatePublic(name = "headline")
  private String title;

  private String editorPhone;

  @FreezerplatePublic
  @ManyToOne
  @JoinColumn(name = "publisher_id")
  private Publisher publisher;

  protected Magazine() {}

  public Magazine(String title, String editorPhone, Publisher publisher) {
    this.title = title;
    this.editorPhone = editorPhone;
    this.publisher = publisher;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getEditorPhone() {
    return editorPhone;
  }

  public void setEditorPhone(String editorPhone) {
    this.editorPhone = editorPhone;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  public void setPublisher(Publisher publisher) {
    this.publisher = publisher;
  }
}
