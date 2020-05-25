package com.herocc.school.aspencheck;

public class JSONReturn extends TimestampedObject {
  public Object data;
  public ErrorInfo errors;

  public JSONReturn(Object data, ErrorInfo errors) {
    this.data = data;
    this.errors = errors;
  }
}
