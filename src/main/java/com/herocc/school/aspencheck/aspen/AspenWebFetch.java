package com.herocc.school.aspencheck.aspen;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.District;
import com.herocc.school.aspencheck.GenericWebFetch;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AspenWebFetch extends GenericWebFetch {
  private String aspenBaseUrl;
  public String districtName;
  public String username;
  public String password;

  private Connection.Response homePage;
  private Connection.Response courseListPage;
  private Connection.Response schedulePage;
  private Connection.Response studentInfoPage;
  public Document lastAssignmentDocument;

  public AspenWebFetch(String dName, String username, String password) {
    this.aspenBaseUrl = "https://" + dName + ".myfollett.com/aspen";
    this.districtName = dName;
    this.username = username;
    this.password = password;

    if (AspenCheck.config.districts.containsKey(dName)) {
      District d = AspenCheck.config.districts.get(dName);
      aspenBaseUrl = d.aspenBaseUrl;
    }

    webUserAgent = webUserAgent + new Random().nextInt(Integer.MAX_VALUE);
    this.login(username, password);
  }

  public AspenWebFetch(District district) {
    this.aspenBaseUrl = district.aspenBaseUrl;
    this.districtName = district.districtName;
    this.login(district.aspenUsername, district.aspenPassword);
  }


  public void login(String username, String password) {
    try {
      String loginUrl = aspenBaseUrl + "/logon.do";

      AspenCheck.log.info("(" + hashCode() + ") old cookies = " + demCookies);
      Connection.Response loginPageResponse = getPage(loginUrl);

      if (loginPageResponse.statusCode() == 404) {
        AspenCheck.log.warning("No login page found at " + aspenBaseUrl);
        return;
      }

      Map<String, String> loginPageCookies = loginPageResponse.cookies();
      AspenCheck.log.info("(" + hashCode() + ") adding login page cookies, cookies = " + loginPageCookies);
      //demCookies.putAll(loginPageCookies);

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

      AspenCheck.log.info("(" + hashCode() + ") old cookies = " + demCookies);
      Connection.Response responsePostLogin = getPage(loginUrl, mapParams);

/*      Connection.Response responsePostLogin = Jsoup.connect(loginUrl)
        .method(Connection.Method.POST)
        .referrer(loginUrl)
        .userAgent(AspenCheck.config.webUserAgent)
        .timeout(10 * 1000)
        .data(mapParams)
        .cookies(demCookies)
        .followRedirects(true)
        .ignoreHttpErrors(true)
        .execute();*/

      Map<String, String> loggedInCookies = responsePostLogin.cookies();
      AspenCheck.log.info("(" + hashCode() + ") adding home page cookies, cookies = " + loggedInCookies);
      //demCookies.putAll(loggedInCookies);

      //AspenCheck.log.info("loginResponse (" + responsePostLogin.url() + ") = " + responsePostLogin.body());
      if (responsePostLogin.statusCode() == 500) AspenCheck.log.warning("Username or Pass incorrect");
      else AspenCheck.log.info("logged in " + responsePostLogin.parse().getElementsByClass("applicationSubtitle").first().child(0).text());

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Boolean areCredsCorrect() {
    try {
      Connection.Response homePage = getPage(aspenBaseUrl + "/home.do", true);
      if (this.homePage == null) {
        this.homePage = homePage;
      }
      if (homePage.statusCode() == 200 || AspenCheck.isOutOfSyncError(homePage.body())){
        return true; // out of sync error is fixed on next page request
      } else {
        System.out.println("error on onCheckCreds");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public Connection.Response getHomePage() {
    if (homePage != null) return homePage;
    try {
      homePage = getPage(aspenBaseUrl + "/home.do");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getRecentAssignmentsPage() {
    try {
      Map<String, String> params = new HashMap<>();
      params.put("preferences", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><preference-set><pref id=\"dateRange\" type=\"int\">4</pref></preference-set>");
      //getPage(aspenBaseUrl + "/studentRecentActivityWidget.do?preferences=<%3Fxml+version%3D\"1.0\"+encoding%3D\"UTF-8\"%3F>%0D%0A<preference-set><pref+id%3D\"dateRange\"+type%3D\"int\">4<%2Fpref><%2Fpreference-set>&rand=1590349426151");
      return getPage(aspenBaseUrl + "/studentRecentActivityWidget.do", params);
    } catch (IOException e) {
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
    String url = aspenBaseUrl + "/portalClassList.do?navkey=academics.classes.list&maximized=true";
    try {
      getPage(url);
      AspenCheck.log.info("getting " + url);
      AspenCheck.log.info("cookies = " + demCookies);
      courseListPage = getPage(url);
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
    String url = aspenBaseUrl + "/portalAssignmentList.do?navkey=academics.classes.list.gcd&maximized=true";
    Map<String, String> map = new HashMap<>();
    map.put("oid", courseId);
    try {
      getPage(url, map);
      return getPage(url, map);
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

  public Connection.Response getAssignmentPage(String assignmentID) {
    String url = aspenBaseUrl + "/portalAssignmentList.do";
    Map<String, String> params = new HashMap<>();
    params.put("userEvent", "2100");
    params.put("userParam", assignmentID);

    Map<String, String> jumpAssignmentParams = new HashMap<>();
    jumpAssignmentParams.put("userEvent", "930");
    jumpAssignmentParams.put("singleSelection", assignmentID);

    try {
      //getPage(url, resetParams);
/*      if (lastAssignmentDocument != null) {
        getPage(aspenBaseUrl + "/jumpToPicklist.do", jumpAssignmentParams);
        params.clear();
      }*/
      Connection.Response assignmentPage = getPage(url, params);
      lastAssignmentDocument = assignmentPage.parse();
      return assignmentPage;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Connection.Response getNextAssignmentPage() {
    String url = aspenBaseUrl + "/portalAssignmentDetail.do";
    try {
      Map<String, String> params = Collections.singletonMap("userEvent", "60");
      Connection.Response assignmentPage = getPage(url, params);
      //lastAssignmentDocument = assignmentPage.parse();
      //AspenCheck.log.info("lastAssignmentDocument is now for " + lastAssignmentDocument.getElementById("propertyValue(gcdColName)-span").text());
      return assignmentPage;
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
}
