package com.herocc.school.aspencheck.aspen.course.assignment;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.ErrorInfo;
import com.herocc.school.aspencheck.JSONReturn;
import com.herocc.school.aspencheck.aspen.AspenWebFetch;
import com.herocc.school.aspencheck.aspen.course.Course;
import org.jsoup.Connection;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.herocc.school.aspencheck.aspen.course.AspenCoursesController.getCourse;

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
public class AspenCourseAssignmentController {

  @RequestMapping("/course/{course-id}/assignment")
  public ResponseEntity<JSONReturn> serveAssignmentList(@PathVariable(value="district-id") String districtName,
                                                        @PathVariable(value="course-id") String course,
                                                        @RequestHeader(value="ASPEN_UNAME", required=false) String u,
                                                        @RequestHeader(value="ASPEN_PASS", required=false) String p,
                                                        @RequestParam(value="term", required = false) String term){

    if (u != null && p != null) {
      AspenWebFetch aspenWebFetch = new AspenWebFetch(districtName, u, p);
      if (!aspenWebFetch.areCredsCorrect()){
        return new ResponseEntity<>(new JSONReturn(null, new ErrorInfo("Invalid Credentials", 500, "Username or password is incorrect")), HttpStatus.UNAUTHORIZED);
      }
      List<Assignment> a = getAssignmentList(aspenWebFetch, getCourse(aspenWebFetch, course, null), term);
      if (a == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONReturn(null, new ErrorInfo("j", 9, "b")));
      return new ResponseEntity<>(new JSONReturn(a, new ErrorInfo()), HttpStatus.OK);
    } else {
      return AspenCheck.invalidCredentialsResponse();
    }
  }

  public static List<Assignment> getAssignmentList(AspenWebFetch a, Course course, String term){
    List<Assignment> assignments = new ArrayList<>();
    Connection.Response assignmentsPage;
    if (Arrays.asList("1", "2", "3", "4").contains(term)){
      assignmentsPage = a.getCourseAssignmentsPage(course.id, Integer.parseInt(term));
    }else {
       assignmentsPage = a.getCourseAssignmentsPage(course.id);
    }

    if (assignmentsPage != null) {
      try {
        for (Element assignmentRow : assignmentsPage.parse().body().getElementsByAttributeValueContaining("class", "listCell listRowHeight")) {
          if (!assignmentRow.text().trim().contains("No matching records")) assignments.add(new Assignment(assignmentRow, AspenCheck.config.districts.get(a.districtName).gradeScale));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    AssignmentInfoFetcher.getAdditionalInfoForAssignments(a, assignments);
    return assignments;
  }
}
