package com.cottagecoders;

import com.opencsv.bean.CsvBindByPosition;

public class KBArticle {
  @CsvBindByPosition(position = 2)
  String title;
  @CsvBindByPosition(position = 3)
  String url;
  @CsvBindByPosition(position = 4)
  String draftUrl;
  @CsvBindByPosition(position = 5)
  String rateTheLLM;
  @CsvBindByPosition(position = 1)
  String author;
  @CsvBindByPosition(position = 0)
  String done = "";

  public KBArticle(String title, String author, String url, String draftUrl) {
    this.title = title;
    this.author = author;
    this.url = url;
    this.draftUrl = draftUrl;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDone() {
    return done;
  }

  public void setDone(String done) {
    this.done = done;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDraftUrl() {
    return draftUrl;
  }

  public void setDraftUrl(String draftUrl) {
    this.draftUrl = draftUrl;
  }

  public String getRateTheLLM() {
    return rateTheLLM;
  }

  public void setRateTheLLM(String RateTheLLM) {
    this.rateTheLLM = RateTheLLM;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }
}
