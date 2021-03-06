package com.herocc.school.aspencheck;

import com.herocc.school.aspencheck.aspen.schedule.AspenScheduleController;
import com.herocc.school.aspencheck.aspen.schedule.Schedule;
import com.herocc.school.aspencheck.calendar.CalendarController;
import com.herocc.school.aspencheck.calendar.Event;

import java.util.*;
import java.util.logging.Level;

import static com.herocc.school.aspencheck.GenericEventGenerator.SourceType;

public class District extends TimestampedObject {

  // Public Information
  public Schedule schedule;
  public ArrayList<Event> events;

  // Configs
  public String districtName;
  public String aspenBaseUrl;

  public String aspenUsername;
  public String aspenPassword;

  public Map<SourceType, List<String>> announcementsSources;

  public Map<String, Integer> columnOrganization = defaultColumnOrganization();
  public Map<String, Double> gradeScale;
  public String schedulePage = defaultSchedulePage;

  public District() {
    asOf = 0;
    Timer autoRefresh = new Timer();
    new Thread(() -> {
      try {
        Thread.sleep(3000);
        if (!checkCredsValid()) {
          AspenCheck.log.warning(districtName + " doesn't have a valid aspen login!");
          AspenCheck.rollbar.warning(districtName + " doesn't have a valid aspen login!");
        } else {
          // Only auto-refresh district if there is a configured login
          autoRefresh.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              refresh();
            }
          }, 0, AspenCheck.config.refreshInterval * 1000);
        }
      } catch (InterruptedException ignored) {}
    }).start();
    // We need to use a thread here because if we didn't, Jackson wouldn't have finished serializing the object
  }

  public void refresh() {
    AspenCheck.log.log(Level.INFO, "Refreshing " + districtName + "'s info, " + AspenCheck.getUnixTime() + " > " + asOf);
    asOf = AspenCheck.getUnixTime();

    new Thread(() -> {
      Thread scheduleThread = new Thread(() -> AspenScheduleController.refreshSchedule(this));
      Thread calendarThread = new Thread(() -> CalendarController.refreshEvents(this));

      calendarThread.start();
      if (checkCredsValid()) {
        scheduleThread.start(); // Don't try to get the schedule if we don't have a login
        AspenCheck.log.fine(districtName + "'s schedule not being fetched due to invalid credentials");
      }

      try {
        scheduleThread.join();
        calendarThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
  }

  private boolean checkCredsValid() {
    aspenUsername = AspenCheck.getEnvFromKey(aspenUsername);
    aspenPassword = AspenCheck.getEnvFromKey(aspenPassword);

    return !(AspenCheck.isNullOrEmpty(aspenUsername) || AspenCheck.isNullOrEmpty(aspenPassword));
  }

  @Override
  public String toString() {
    return "District{" +
      "schedule=" + schedule +
      ", events=" + events +
      ", districtName='" + districtName + '\'' +
      ", aspenBaseUrl='" + aspenBaseUrl + '\'' +
      ", aspenUsername='" + aspenUsername + '\'' +
      ", aspenPassword='" + aspenPassword + '\'' +
      ", announcementsSources=" + announcementsSources +
      ", columnOrganization=" + columnOrganization +
      '}';
  }

  public static Map<String, Integer> defaultColumnOrganization(){
    Map<String, Integer> map = new HashMap<>();
    map.put("className", 1);
    map.put("id", 1);
    map.put("courseCode", 2);
    map.put("term", 4);
    map.put("teacher", 5);
    map.put("room", 6);
    map.put("termGrade", 7);
    return map;
  }

  public static String defaultSchedulePage = "/studentScheduleContextList.do?navkey=myInfo.sch.list";
}
