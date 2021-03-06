package com.herocc.school.aspencheck;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GenericWebFetch {
  public String webUserAgent = AspenCheck.config.webUserAgent;
  public Map<String, String> demCookies = new HashMap<>();

  public Connection.Response getPage(String url) throws IOException {
    return getPage(url, Collections.emptyMap());
  }

  public Connection.Response getPage(String url, boolean ignoreHttpErrors) throws IOException {
    return getPage(url, Collections.emptyMap(), ignoreHttpErrors);
  }

  public Connection.Response getPage(String url, Map<String, String> formData) throws IOException {
    return getPage(url, formData, true);
  }

  public Connection.Response getPage(String url, Map<String, String> formData, boolean ignoreHttpErrors) throws IOException {
    int attempts = 0;
    Connection.Response success = null;
    while (success == null){
      try {
        success = Jsoup.connect(url)
                .userAgent(webUserAgent)
                .timeout(10 * 1000)
                .data(formData)
                .cookies(demCookies)
                .followRedirects(true)
                .ignoreHttpErrors(ignoreHttpErrors)
                .execute();
      } catch (HttpStatusException e){
        e.printStackTrace();
        attempts++;
        if (attempts >= 5){
          throw e;
        }
      }
    }
    return success;
  }

  public static String getURL(String url) {
    GenericWebFetch g = new GenericWebFetch();
    try {
      return g.getPage(url).body();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
