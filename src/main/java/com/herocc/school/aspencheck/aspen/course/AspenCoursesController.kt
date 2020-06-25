package com.herocc.school.aspencheck.aspen.course

import com.herocc.school.aspencheck.AspenCheck
import com.herocc.school.aspencheck.District
import com.herocc.school.aspencheck.ErrorInfo
import com.herocc.school.aspencheck.JSONReturn
import com.herocc.school.aspencheck.aspen.AspenWebFetch
import com.herocc.school.aspencheck.aspen.course.assignment.Assignment
import com.herocc.school.aspencheck.aspen.course.assignment.AssignmentInfoFetcher
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
open class AspenCoursesController {

    @Async
    @RequestMapping("/course")
    fun serveCourseList(
        @PathVariable(value = "district-id") districtName: String,
        @RequestParam(value = "moreData", defaultValue = "false") moreData: String,
        @RequestHeader(value = "ASPEN_UNAME", required = false) u: String?,
        @RequestHeader(value = "ASPEN_PASS", required = false) p: String?,
        @RequestParam(value = "term", required = false) term: String?
    ): CompletableFuture<ResponseEntity<JSONReturn>> {
        return if (u != null && p != null) {
            val coursesManager = AspenCoursesManager(u, p, AspenCheck.getDistrictByName(districtName))
            if (!coursesManager.aspenWebFetch.areCredsCorrect()) {
                AspenCheck.incorrectCredentialsResponse()
            } else {
                val courses = coursesManager.getCourses(moreData.toLowerCase()=="true", term)
                if (moreData=="true") {
                    AspenCheck.log.info("all moredata gotten = ${courses.all { it.assignments!=null }}")
                }
                CompletableFuture.completedFuture(ResponseEntity(JSONReturn(courses, ErrorInfo()), HttpStatus.OK))
            }
        } else {
            AspenCheck.invalidCredentialsResponse()
        }
    }

    @Async
    @RequestMapping("/course/{course-id}")
    fun serveCourseInfo(
        @PathVariable(value = "district-id") districtName: String?,
        @PathVariable(value = "course-id") courseID: String?,
        @RequestHeader(value = "ASPEN_UNAME", required = false) u: String?,
        @RequestHeader(value = "ASPEN_PASS", required = false) p: String?,
        @RequestParam(value = "term", required = false) term: String?
    ): CompletableFuture<ResponseEntity<JSONReturn>> {
        return if (u!=null && p!=null) {
            val coursesManager = AspenCoursesManager(u, p, AspenCheck.getDistrictByName(districtName))
            if (!coursesManager.aspenWebFetch.areCredsCorrect()) {
                return AspenCheck.incorrectCredentialsResponse()
            }
            val course = coursesManager.getCourses(true, term).find { it.id==courseID }
                ?: return AspenCheck.incorrectCourseIDResponse()
            CompletableFuture.completedFuture(ResponseEntity(JSONReturn(course, ErrorInfo()), HttpStatus.OK))
        } else {
            AspenCheck.invalidCredentialsResponse()
        }
    }
}

class AspenCoursesManager(
    private val username: String,
    private val password: String,
    private val district: District,
    val aspenWebFetch: AspenWebFetch = AspenWebFetch(district.districtName, username, password)
) {

    fun getCourses(moreData: Boolean = false, term: String? = null): List<Course> {
        AspenCheck.log.info("getting courses for " + district.districtName + ", term = " + term)
        val classListPage: Connection.Response = when (term) {
            in listOf("1", "2", "3", "4") -> aspenWebFetch.getCourseListPage(term!!.toInt())
            else -> aspenWebFetch.getCourseListPage()
        }
        try {
            val courseElements = classListPage.parse().body().getElementsByAttributeValueContaining("class", "listCell listRowHeight")
            if(courseElements[0].getElementsContainingOwnText("No matching records").size > 0){
                return getCourses(moreData, "4")
            }
            val courses = courseElements.map { classRow ->
                Course(classRow, district.columnOrganization)
            }
            if (moreData) {
                getMoreInfoCourses(courses, term)
            }
            return courses
        } catch (e: IOException) {
            e.printStackTrace()
            AspenCheck.rollbar.error(e, "Error while parsing CourseList of user from " + district.districtName)
            return emptyList()
        }
    }

    fun getMoreInfoCourses(courses: List<Course>, term: String?) {
        runBlocking {
            val webFetches = (0 until 4).map { AspenWebFetch(district.districtName, username, password) }
            val jobs = courses.chunked(courses.size / webFetches.size).mapIndexed { i, coursesPerWebFetch ->
                CoroutineScope(Dispatchers.IO).launch {
                    val webFetch = webFetches[i]
                    if(term != null){
                        webFetch.getCourseListPage(term.toInt())
                    }
                    coursesPerWebFetch.forEach { course ->
                        //AspenCheck.log.info("webFetch for ${course.name} login = ${webFetch.areCredsCorrect()}")
                        course.getMoreInformation(webFetch, term)
                        //AspenCheck.log.info("webFetch after for ${course.name} login = ${webFetch.areCredsCorrect()}")
                    }
                }
            }
            jobs.joinAll()
        }
    }
}

fun getAssignmentList(a: AspenWebFetch, course: Course, term: String?): List<Assignment> {
    val assignmentsPage: Connection.Response = when (term) {
        in listOf("1", "2", "3", "4") -> a.getCourseAssignmentsPage(course.id, term!!.toInt())
        else -> a.getCourseAssignmentsPage(course.id)
    } ?: return emptyList()

    try {
        val assignmentElements = assignmentsPage.parse().body().getElementsByAttributeValueContaining("class", "listCell listRowHeight")
        val assignments = assignmentElements.map { assignmentRow ->
            if (assignmentRow.text().contains("No matching records")) return@map null
            Assignment(assignmentRow, AspenCheck.config.districts[a.districtName]!!.gradeScale)
        }.filterNotNull()

        AssignmentInfoFetcher().getAdditionalInfoForAssignments(a, course.id, assignments)
        return assignments

    } catch (e: IOException) {
        e.printStackTrace()
        return emptyList()
    }
}