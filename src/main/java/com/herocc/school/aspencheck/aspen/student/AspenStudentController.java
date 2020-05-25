package com.herocc.school.aspencheck.aspen.student;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.ErrorInfo;
import com.herocc.school.aspencheck.JSONReturn;
import com.herocc.school.aspencheck.aspen.AspenWebFetch;
import org.jsoup.Connection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
public class AspenStudentController {

  @RequestMapping("student")
  public ResponseEntity<JSONReturn> serveSchedule(@PathVariable(value="district-id") String districtName,
                                  @RequestHeader(value="ASPEN_UNAME", required=false) String u,
                                  @RequestHeader(value="ASPEN_PASS", required=false) String p) {
    AspenCheck.log.info("getting " + districtName + " student with username " + u);
    if (u != null && p != null) {
      return new ResponseEntity<>(new JSONReturn(getStudent(districtName, u, p), new ErrorInfo()), HttpStatus.OK);
    } else {
      return AspenCheck.invalidCredentialsResponse();
    }
  }

  public static Student getStudent(String districtName, String u, String p) {
    AspenCheck.log.info("getting " + districtName + " student with username " + u);
    AspenWebFetch aspenWebFetch = new AspenWebFetch(districtName, u, p);
    Connection.Response studentPage = aspenWebFetch.getStudentInfoPage();
    if (studentPage != null) {
      try {
        return new Student(studentPage.parse());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
