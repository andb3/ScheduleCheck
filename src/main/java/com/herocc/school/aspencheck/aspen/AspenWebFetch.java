package com.herocc.school.aspencheck.aspen;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.District;
import com.herocc.school.aspencheck.GenericWebFetch;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AspenWebFetch extends GenericWebFetch {
  private String aspenBaseUrl;
  public String districtName;

  private Connection.Response homePage;
  private Connection.Response courseListPage;
  private Connection.Response schedulePage;
  private Connection.Response studentInfoPage;

  public AspenWebFetch(String dName, String username, String password) {
    this.aspenBaseUrl = "https://" + dName + ".myfollett.com/aspen";
    this.districtName = dName;

    if (AspenCheck.config.districts.containsKey(dName)) {
      District d = AspenCheck.config.districts.get(dName);
      aspenBaseUrl = d.aspenBaseUrl;
    }

    this.login(username, password);
  }

  public AspenWebFetch(District district) {
    this.aspenBaseUrl = district.aspenBaseUrl;
    this.districtName = district.districtName;
    this.login(district.aspenUsername, district.aspenPassword);
  }

  public Boolean areCredsCorrect() {
    try {
      Connection.Response homePage = getPage(aspenBaseUrl + "/home.do");
      if (this.homePage == null){
        this.homePage = homePage;
      }
      return homePage.statusCode() == 200;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public Connection.Response getHomePage() {
    if (homePage != null) return homePage;
    try {
      homePage = getPage(aspenBaseUrl + "/home.do");
    } catch (IOException e){
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getRecentAssignmentsPage(){
    try {
      Map<String, String> params = new HashMap<>();
      params.put("preferences", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><preference-set><pref id=\"dateRange\" type=\"int\">4</pref></preference-set>");
      //getPage(aspenBaseUrl + "/studentRecentActivityWidget.do?preferences=<%3Fxml+version%3D\"1.0\"+encoding%3D\"UTF-8\"%3F>%0D%0A<preference-set><pref+id%3D\"dateRange\"+type%3D\"int\">4<%2Fpref><%2Fpreference-set>&rand=1590349426151");
      return getPage(aspenBaseUrl + "/studentRecentActivityWidget.do", params);
    } catch (IOException e){
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getStudentInfoPage() {
    if (studentInfoPage != null) return studentInfoPage;
    try {
      studentInfoPage = getPage(aspenBaseUrl + "/portalStudentDetail.do?navkey=myInfo.details.detail&maximized=true");
      return studentInfoPage;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getCourseListPage() {
    if (courseListPage != null) return courseListPage;
    try {
      AspenCheck.log.info("getting " + aspenBaseUrl + "/portalClassList.do?navkey=academics.classes.list&maximized=true");
      AspenCheck.log.info("cookies = " + demCookies);
      courseListPage = getPage(aspenBaseUrl + "/portalClassList.do?navkey=academics.classes.list&maximized=true");
      AspenCheck.log.info("" + courseListPage.statusCode());
      return courseListPage;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getCourseListPage(int term) {
    if (courseListPage != null) return courseListPage;
    String url = aspenBaseUrl + "/portalClassList.do?navkey=academics.classes.list&maximized=true";
    try {
      AspenCheck.log.info("getting " + aspenBaseUrl + "/portalClassList.do?navkey=academics.classes.list&maximized=true");
      getPage(url);
      Map<String, String> formData = new HashMap<>();
      formData.put("userEvent", "950");
      formData.put("termFilter", "GTMP10000000Q" + term);
      courseListPage = getPage(url, formData);
      return courseListPage;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  public Connection.Response getCourseInfoPage(String courseId) {
    Map<String, String> formData = new HashMap<>();
    //formData.put("userEvent", "2210");
    //formData.put("userParam", courseId);
    formData.put("selectedStudentOid", courseId);
    try {
      //getCourseAssignmentsPage(courseId);
      return getPage(aspenBaseUrl + "/portalClassDetail.do?navkey=academics.classes.list.detail&maximized=true", formData);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getCourseAssignmentsPage(String courseId) {
    Map<String, String> map = new HashMap<>();
    map.put("oid", courseId);
    try {
      return getPage(aspenBaseUrl + "/portalAssignmentList.do?navkey=academics.classes.list.gcd&maximized=true", map);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  public Connection.Response getCourseAssignmentsPage(String courseId, int term) {
    Map<String, String> map = new HashMap<>();
    map.put("oid", courseId);
    String url = aspenBaseUrl + "/portalAssignmentList.do?navkey=academics.classes.list.gcd&maximized=true";
    try {
      // when term is switched, Aspen actually gets the term for the most recently opened class
      // (since the only way a user could change term on the website would be to already have the class page open)
      // so, we open the default class page first and then switch the term
      getPage(url, map);

      map.put("gradeTermOid", "GTMP10000000Q" + term);
      map.put("userEvent", "2210");

      return getPage(url, map);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getSchedulePage() {
    if (schedulePage != null) return schedulePage;
    String scheduleUrl = (AspenCheck.config.districts.containsKey(districtName)) ? AspenCheck.config.districts.get(districtName).schedulePage : District.defaultSchedulePage;
    Map<String, String> params = new HashMap<>();
    params.put("userEvent", "360");
    try {
      schedulePage = getPage(aspenBaseUrl + scheduleUrl);
      return schedulePage;
    } catch (HttpStatusException e) {
      if (e.getStatusCode() == 404 || e.getStatusCode() == 500) {
        AspenCheck.log.warning("This login doesn't have a schedule page!");
      } else {
        AspenCheck.log.warning("Login details incorrect, or Aspen is having issues, please try again later!");
      }
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void login(String username, String password) {
    try {
      String loginUrl = aspenBaseUrl + "/logon.do";
      Connection.Response loginPageResponse = Jsoup.connect(loginUrl)
                      .userAgent(AspenCheck.config.webUserAgent)
                      .timeout(10 * 1000)
                      .followRedirects(true)
                      .execute();

      if (loginPageResponse.statusCode() == 404) {
        AspenCheck.log.warning("No login page found at " + aspenBaseUrl);
        return;
      }

      Map<String, String> mapLoginPageCookies = loginPageResponse.cookies();
      Map<String, String> mapParams = new HashMap<>();
      mapParams.put("deploymentId", loginPageResponse.parse().getElementById("deploymentId").attr("value").trim());
      mapParams.put("userEvent", "930");
      mapParams.put("username", username);
      mapParams.put("password", password);
      mapParams.put("mobile", "false");

      if (username == null || password == null) {
        AspenCheck.log.warning("Invalid Username or Password!");
        return;
      }

      Connection.Response responsePostLogin = Jsoup.connect(loginUrl)
              .referrer(loginUrl)
              .userAgent(AspenCheck.config.webUserAgent)
              .timeout(10 * 1000)
              .data(mapParams)
              .cookies(mapLoginPageCookies)
              .followRedirects(true)
              .execute();

      Map<String, String> mapLoggedInCookies = responsePostLogin.cookies();
      demCookies.putAll(mapLoggedInCookies);
      demCookies.putAll(mapLoginPageCookies);

      if (responsePostLogin.statusCode() == 500) AspenCheck.log.warning("Username or Pass incorrect");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
