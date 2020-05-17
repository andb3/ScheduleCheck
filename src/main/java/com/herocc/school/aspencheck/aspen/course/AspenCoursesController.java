package com.herocc.school.aspencheck.aspen.course;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.District;
import com.herocc.school.aspencheck.ErrorInfo;
import com.herocc.school.aspencheck.JSONReturn;
import com.herocc.school.aspencheck.aspen.AspenWebFetch;
import org.jsoup.Connection;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
public class AspenCoursesController {

  @RequestMapping("/course")
  public ResponseEntity<JSONReturn> serveSchedule(@PathVariable(value = "district-id") String districtName,
                                                  @RequestParam(value = "moreData", defaultValue = "false") String moreData,
                                                  @RequestHeader(value = "ASPEN_UNAME", required = false) String u,
                                                  @RequestHeader(value = "ASPEN_PASS", required = false) String p,
                                                  @RequestParam(value = "term", required = false) String term) {

    if (u != null && p != null) {
      return new ResponseEntity<>(new JSONReturn(getCourses(new AspenWebFetch(districtName, u, p), moreData.equals("true"), term), new ErrorInfo()), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new JSONReturn(null, new ErrorInfo("Invalid Credentials", 0, "No username or password given")), HttpStatus.UNAUTHORIZED);
    }
  }

  @RequestMapping("/course/{course-id}")
  public ResponseEntity<JSONReturn> serveCourseInfo(@PathVariable(value = "district-id") String districtName,
                                                    @PathVariable(value = "course-id") String course,
                                                    @RequestHeader(value = "ASPEN_UNAME", required = false) String u,
                                                    @RequestHeader(value = "ASPEN_PASS", required = false) String p,
                                                    @RequestParam(value = "term", required = false) String term) {

    if (u != null && p != null) {
      AspenWebFetch a = new AspenWebFetch(districtName, u, p);
      Course c = getCourse(a, course, term).getMoreInformation(a, term);
      if (c == null)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONReturn(null, new ErrorInfo("Course not Found", 404, "The course you tried to fetch doesn't exist or was inaccessible")));
      return new ResponseEntity<>(new JSONReturn(c, new ErrorInfo()), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new JSONReturn(null, new ErrorInfo("Invalid Credentials", 0, "No username or password given")), HttpStatus.UNAUTHORIZED);
    }
  }

  public static List<Course> getCourses(AspenWebFetch a) { return getCourses(a, false); }

  public static List<Course> getCourses(AspenWebFetch a, boolean moreData) { return getCourses(a, moreData, null); }

  public static List<Course> getCourses(AspenWebFetch a, boolean moreData, String term) {
    AspenCheck.log.info("getting courses for " + a.districtName + ", term = " + term);
    Connection.Response classListPage;
    if(Arrays.asList("1", "2", "3", "4").contains(term)){
      classListPage = a.getCourseListPage(Integer.parseInt(term));
    }else {
      classListPage = a.getCourseListPage();
    }
    List<Course> courses = new ArrayList<>();
    if (classListPage != null) {
      try {
        for (Element classRow : classListPage.parse().body().getElementsByAttributeValueContaining("class", "listCell listRowHeight")) {
          Map<String, Integer> columnOrganization = AspenCheck.config.districts.containsKey(a.districtName) ? AspenCheck.config.districts.get(a.districtName).columnOrganization : District.defaultColumnOrganization();
          Course c = new Course(classRow, columnOrganization);
          c = moreData ? c.getMoreInformation(a, term) : c;
          courses.add(c);
        }
      } catch (IOException e) {
        e.printStackTrace();
        AspenCheck.rollbar.error(e, "Error while parsing CourseList of user from " + a.districtName);
      }
    }
    return courses;
  }

  public static Course getCourse(AspenWebFetch a, String courseId, boolean moreData, String term) {
    List<Course> enrolledCourses = getCourses(a);

    for (Course c : enrolledCourses) {
      if (c.id.equalsIgnoreCase(courseId) || c.code.equalsIgnoreCase(courseId) || c.name.equalsIgnoreCase(courseId))
        return moreData ? c.getMoreInformation(a, term) : c;
    }
    return null;
  }

  public static Course getCourse(AspenWebFetch a, String courseId, String term) {
    return getCourse(a, courseId, true, term);
  }
}
