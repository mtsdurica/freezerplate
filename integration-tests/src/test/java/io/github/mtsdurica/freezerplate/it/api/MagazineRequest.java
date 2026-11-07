package io.github.mtsdurica.freezerplate.it.api;

import jakarta.validation.constraints.NotBlank;

public class MagazineRequest {

  @NotBlank private String headline;

  private String editorPhone;

  private Integer publisherId;

  public MagazineRequest() {}

  public MagazineRequest(String headline, String editorPhone, Integer publisherId) {
    this.headline = headline;
    this.editorPhone = editorPhone;
    this.publisherId = publisherId;
  }

  public String getHeadline() {
    return headline;
  }

  public void setHeadline(String headline) {
    this.headline = headline;
  }

  public String getEditorPhone() {
    return editorPhone;
  }

  public void setEditorPhone(String editorPhone) {
    this.editorPhone = editorPhone;
  }

  public Integer getPublisherId() {
    return publisherId;
  }

  public void setPublisherId(Integer publisherId) {
    this.publisherId = publisherId;
  }
}
