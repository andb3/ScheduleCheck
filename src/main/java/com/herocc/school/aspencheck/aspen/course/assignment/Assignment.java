package com.herocc.school.aspencheck.aspen.course.assignment;

import com.herocc.school.aspencheck.AspenCheck;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class Assignment {

  public String id;
  public String name;
  public String credit;
  public String dateAssigned;
  public String dateDue;
  public String feedback;
  public String score;
  public String possibleScore;
  public String gradeLetter;


  public Assignment(Element e, Map<String, Double> gradeScale){
    Elements tdTags = e.getElementsByTag("td");
    id = tdTags.get(1).id().trim();
    name = tdTags.get(1).text().trim();
    dateAssigned = tdTags.get(2).text().trim();
    dateDue = tdTags.get(3).text().trim();

    String possiblePercent = tdTags.get(4).getElementsByAttributeValueContaining("class", "percentFieldInlineLabel").text().trim();
    if (!AspenCheck.isNullOrEmpty(possiblePercent)) {
      credit = possiblePercent;
      String scoreColumnText = tdTags.get(4).getElementsByTag("td").get(2).text().trim();
      int slashIndex = scoreColumnText.indexOf('/');
      score = scoreColumnText.substring(0, slashIndex).trim();
      possibleScore = scoreColumnText.substring(slashIndex + 1).trim();
    } else { //Ungraded
      credit = tdTags.get(4).text().trim();
    }

    if (gradeScale != null && !gradeScale.isEmpty() && score != null){
      gradeLetter = findLetter(Double.parseDouble(score)/Double.parseDouble(possibleScore), new ArrayList<>(gradeScale.entrySet()));
    }

    // feedback = tdTags.get(5).text().trim(); // TODO when empty, returns percentCredit
  }

  private static String findLetter(double grade, List<Map.Entry<String, Double>> gradeScale){
    double percent = grade * 100;
    gradeScale.sort(Comparator.comparingDouble(Map.Entry::getValue));
    System.out.println("finding letter for " + grade + ", gradeScale = " + gradeScale);
    for (int i = 0; i < gradeScale.size(); i++) {
      Map.Entry<String, Double> entry = gradeScale.get(i);
      if (i == gradeScale.size() - 1) return  entry.getKey();
      Map.Entry<String, Double> next = gradeScale.get(i + 1);
      if (entry.getValue() < percent && percent < next.getValue()) {
        return entry.getKey();
      }
    }
    return null;
  }
}
