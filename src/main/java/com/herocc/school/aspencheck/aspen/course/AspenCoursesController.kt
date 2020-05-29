package com.herocc.school.aspencheck.aspen.course

import com.herocc.school.aspencheck.AspenCheck
import com.herocc.school.aspencheck.District
import com.herocc.school.aspencheck.ErrorInfo
import com.herocc.school.aspencheck.JSONReturn
import com.herocc.school.aspencheck.aspen.AspenWebFetch
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
class AspenCoursesController {
  @RequestMapping("/course")
  fun serveSchedule(
    @PathVariable(value = "district-id") districtName: String,
    @RequestParam(value = "moreData", defaultValue = "false") moreData: String,
    @RequestHeader(value = "ASPEN_UNAME", required = false) u: String?,
    @RequestHeader(value = "ASPEN_PASS", required = false) p: String?,
    @RequestParam(value = "term", required = false) term: String?
  ): ResponseEntity<JSONReturn> {
    return if (u != null && p != null) {
      val aspenWebFetch = AspenWebFetch(districtName, u, p)
      if (!aspenWebFetch.areCredsCorrect()) {
        AspenCheck.incorrectCredentialsResponse()
      } else {
        val courses = getCourses(aspenWebFetch, term)
        if (moreData == "true") getMoreInfoCourses(courses, u, p, districtName, term)
        ResponseEntity(JSONReturn(courses, ErrorInfo()), HttpStatus.OK)
      }
    } else {
      AspenCheck.invalidCredentialsResponse()
    }
  }

  @RequestMapping("/course/{course-id}")
  fun serveCourseInfo(
    @PathVariable(value = "district-id") districtName: String?,
    @PathVariable(value = "course-id") course: String?,
    @RequestHeader(value = "ASPEN_UNAME", required = false) u: String?,
    @RequestHeader(value = "ASPEN_PASS", required = false) p: String?,
    @RequestParam(value = "term", required = false) term: String?
  ): ResponseEntity<JSONReturn> {
    return if (u != null && p != null) {
      val a = AspenWebFetch(districtName, u, p)
      if (!a.areCredsCorrect()) {
        return AspenCheck.incorrectCredentialsResponse()
      }
      val c = getCourse(a, course, false, term)!!.getMoreInformation(a, term)
        ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          JSONReturn(
            null,
            ErrorInfo("Course not Found", 404, "The course you tried to fetch doesn't exist or was inaccessible")
          )
        )
      ResponseEntity(JSONReturn(c, ErrorInfo()), HttpStatus.OK)
    } else {
      AspenCheck.invalidCredentialsResponse()
    }
  }

  companion object {
    fun getCourses(a: AspenWebFetch): List<Course> {
      return getCourses(a, null)
    }

    fun getCourses(a: AspenWebFetch, term: String?): List<Course> {
      AspenCheck.log.info("getting courses for " + a.districtName + ", term = " + term)
      val classListPage: Connection.Response? = if (listOf("1", "2", "3", "4").contains(term)) {
        a.getCourseListPage(term!!.toInt())
      } else {
        a.courseListPage
      }
      val courses: MutableList<Course> = ArrayList()
      if (classListPage != null) {
        try {
          for (classRow in classListPage.parse().body().getElementsByAttributeValueContaining("class", "listCell listRowHeight")) {
            val columnOrganization =
              if (AspenCheck.config.districts.containsKey(a.districtName)) AspenCheck.config.districts[a.districtName]!!.columnOrganization else District.defaultColumnOrganization()
            val c = Course(classRow, columnOrganization)
            courses.add(c)
          }
        } catch (e: IOException) {
          e.printStackTrace()
          AspenCheck.rollbar.error(e, "Error while parsing CourseList of user from " + a.districtName)
        }
      }
      return courses
    }

    fun getMoreInfoCourses(
      courses: List<Course>,
      username: String,
      password: String,
      districtName: String,
      term: String?
    ) {
      runBlocking {
        val webFetches = (0 until 4).map { AspenWebFetch(districtName, username, password) }
        val jobs = courses.chunked(courses.size / webFetches.size).mapIndexed { i, coursesPerWebFetch ->
          CoroutineScope(Dispatchers.IO).launch {
            val webFetch = webFetches[i]
            coursesPerWebFetch.forEach { course ->
              AspenCheck.log.info("webFetch for ${course.name} login = ${webFetch.areCredsCorrect()}")
              course.getMoreInformation(webFetch, term)
              AspenCheck.log.info("webFetch after for ${course.name} login = ${webFetch.areCredsCorrect()}")
            }
          }
        }
        jobs.joinAll()
      }
    }

    fun getCourse(a: AspenWebFetch, courseId: String?, moreData: Boolean, term: String?): Course? {
      val enrolledCourses = getCourses(a)
      for (c in enrolledCourses) {
        if (c.id.equals(courseId, ignoreCase = true)
          || c.code.equals(courseId, ignoreCase = true)
          || c.name.equals(courseId, ignoreCase = true)
        )
          return if (moreData) c.getMoreInformation(a, term) else c
      }
      return null
    }

    @JvmStatic
    fun getCourse(a: AspenWebFetch, courseId: String?, term: String?): Course? {
      return getCourse(a, courseId, true, term)
    }
  }
}
