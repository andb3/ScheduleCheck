package com.herocc.school.aspencheck;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Holds information about errors in a given fetch to return to user
 */
public class ErrorInfo {
  //title: General info about error
  public String title;
  //id: unique id for
  public int id;
  //detail: More specific information about the error.
  public String details;

  public ErrorInfo() { }

  public ErrorInfo(String title, int id, String details) {
    this.title = title;
    this.id = id;
    this.details = details;
  }
}
