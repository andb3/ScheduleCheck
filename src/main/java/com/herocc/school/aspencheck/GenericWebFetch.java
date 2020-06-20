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
    if (AspenCheck.isOutOfSyncError(success.body())){
      new Exception("Aspen returned out of sync error for " + url).printStackTrace();
    } else if (AspenCheck.isNotLoggedOnError(success.body())){
      new Exception("(" + hashCode() + ") Aspen returned not logged in error for " + url).printStackTrace();
    }
    Map<String, String> newCookies = Utils.subtract(success.cookies(), demCookies);
    if (!newCookies.isEmpty()) AspenCheck.log.info("("+ hashCode() +") new cookies = " + newCookies);
    demCookies.putAll(success.cookies());
    return success;
  }
}
