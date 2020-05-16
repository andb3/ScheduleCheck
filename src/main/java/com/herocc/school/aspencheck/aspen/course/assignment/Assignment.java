package com.herocc.school.aspencheck.aspen.course.assignment;

import com.herocc.school.aspencheck.AspenCheck;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Assignment {

  public String id;
  public String name;
  public String credit;
  public String dateAssigned;
  public String dateDue;
  public String feedback;
  public String score;
  public String possibleScore;


  public Assignment(Element e){
    Elements tdTags = e.getElementsByTag("td");
    id = tdTags.get(1).id().trim();
    name = tdTags.get(1).text().trim();
    dateAssigned = tdTags.get(2).text().trim();
    dateDue = tdTags.get(3).text().trim();

    String possiblePercent = tdTags.get(4).getElementsByAttributeValueContaining("class", "percentFieldInlineLabel").text().trim();
    if (!AspenCheck.isNullOrEmpty(possiblePercent)) {
      credit = possiblePercent;
      String scoreColumnText = tdTags.get(4).getElementsByTag("td").get(2).text().trim();
      AspenCheck.log.info("scoreColumnText = " + scoreColumnText);
      int slashIndex = scoreColumnText.indexOf('/');
      score = scoreColumnText.substring(0, slashIndex).trim();
      possibleScore = scoreColumnText.substring(slashIndex + 1).trim();
    } else { //Ungraded
      credit = tdTags.get(4).text().trim();
    }

    // feedback = tdTags.get(5).text().trim(); // TODO when empty, returns percentCredit
  }

}
