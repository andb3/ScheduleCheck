package com.herocc.school.aspencheck.aspen.course;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.aspen.AspenWebFetch;
import com.herocc.school.aspencheck.aspen.course.assignment.AspenCourseAssignmentController;
import com.herocc.school.aspencheck.aspen.course.assignment.Assignment;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Course {
  private Element classInfoPage;

  public String id;
  public String name;
  public String code;
  public String term;
  public String teacher;
  public String room;
  public String currentTermGrade;

  public Map<String, String> postedGrades;
  public Map<String, List<GradeCategory>> categoryTable;
  public List<Assignment> assignments;

  public Course(Element classListRow, Map<String, Integer> columnOrganization) {
    id = classListRow.getElementsByTag("td").get(columnOrganization.get("id")).id().trim();
    name = classListRow.getElementsByTag("td").get(columnOrganization.get("className")).text().trim(); // Also possibly td[3]
    code = classListRow.getElementsByTag("td").get(columnOrganization.get("courseCode")).text().trim();
    term = classListRow.getElementsByTag("td").get(columnOrganization.get("term")).text().trim();
    teacher = classListRow.getElementsByTag("td").get(columnOrganization.get("teacher")).text().trim();
    room = classListRow.getElementsByTag("td").get(columnOrganization.get("room")).text().trim();
    currentTermGrade = classListRow.getElementsByTag("td").get(columnOrganization.get("termGrade")).text().trim();
  }

  public Course getMoreInformation(AspenWebFetch webFetch) {
    return getMoreInformation(webFetch, null);
  }
  public Course getMoreInformation(AspenWebFetch webFetch, String term) {
    try {
      this.assignments = AspenCourseAssignmentController.getAssignmentList(webFetch, this, term); // seems like getCourseInfo only works after the current class is set by opening its assignments
      this.classInfoPage = webFetch.getCourseInfoPage(id).parse().body();
      //AspenCheck.log.info("classInfoPage = " + classInfoPage);
      this.postedGrades = getTermGrades();
      this.categoryTable = getCategoryGrades();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return this;
  }

  private Map<String, String> getTermGrades() {
    Map<String, String> termGrades = new HashMap<>();
    Elements e = classInfoPage.getElementsByAttributeValueContaining("class", "listHeaderText inputGridCellActive listCell").get(0).getElementsByTag("td");
    for (int i = e.size() - 1; i >= 1; i--) { // All elements except for the first one (0)
       termGrades.put(String.valueOf(i), e.get(i).text().trim());
    }
    return termGrades;
  }

  private Map<String, List<GradeCategory>> getCategoryGrades(){
    Map<String, List<GradeCategory>> categoryGrades = new HashMap<>();

    Element categoryTable = classInfoPage
      .getElementsByAttributeValue("id", "dataGrid")
      .get(1); //skip attendance summary

    List<String> categories = categoryTable
      .getElementsByAttributeValue("rowspan", "2")
      .stream()
      .map(e -> e.text().trim())
      .collect(Collectors.toList());

    List<String> terms = categoryTable
      .getElementsByAttributeValueContaining("class", "listHeaderText")
      .stream()
      .filter(e -> !e.hasAttr("colspan") && !e.attr("class").contains("listCell"))
      .map(e -> e.text().trim())
      .collect(Collectors.toList());

    AspenCheck.log.info("categories = " + categories);
    AspenCheck.log.info("terms = " + terms);

    Elements tableCells = categoryTable.getElementsByAttributeValue("class", "listCell");
    //AspenCheck.log.info("tableCells = " + tableCells);

    for (int t = 0; t < terms.size(); t++) {
      String term = terms.get(t);
      List<GradeCategory> categoryList = new ArrayList<>();
      for (int c = 0; c < categories.size(); c++) {
        String categoryName = categories.get(c);
        String weightText = tableCells.get(c * 2).child(t + 2).text().trim(); //skip category name and "weight" label
        String gradeText = tableCells.get(c * 2 + 1).child(t + 1).text().trim(); //skip "avg" label

        String weight = weightText.replaceAll("[^\\d.]", "").trim(); // strip percent
        String grade = gradeText.replaceAll("[^\\d.]", "").trim(); // strip letter
        String letter = gradeText.replace(grade, "").trim();
        GradeCategory category = new GradeCategory(categoryName, weight, grade, letter);
        categoryList.add(category);
      }
      categoryGrades.put(term.substring(term.length() - 1), categoryList);
    }

    return categoryGrades;
  }
}
