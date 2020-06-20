package com.herocc.school.aspencheck.aspen.recent

import com.herocc.school.aspencheck.AspenCheck
import com.herocc.school.aspencheck.ErrorInfo
import com.herocc.school.aspencheck.JSONReturn
import com.herocc.school.aspencheck.aspen.AspenWebFetch
import com.herocc.school.aspencheck.aspen.course.assignment.Assignment
import org.jsoup.nodes.Document
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
public class AspenRecentController {

  @RequestMapping("/recent")
  fun serveRecentAssignments(@PathVariable(value = "district-id") districtName: String,
                             @RequestHeader(value = "ASPEN_UNAME", required = false) username: String?,
                             @RequestHeader(value = "ASPEN_PASS", required = false) password: String?): CompletableFuture<ResponseEntity<JSONReturn>> {
    if (username == null || password == null) return AspenCheck.invalidCredentialsResponse()

    val aspenWebFetch = AspenWebFetch(districtName, username, password)
    return if (!aspenWebFetch.areCredsCorrect()) {
      CompletableFuture.completedFuture(ResponseEntity(JSONReturn(null, ErrorInfo("Invalid Credentials", 500, "Username or password is incorrect")), HttpStatus.UNAUTHORIZED))
    } else {
      val recentAssignments = aspenWebFetch.recentAssignmentsPage
      CompletableFuture.completedFuture(ResponseEntity(JSONReturn(parseRecentAssignments(recentAssignments.parse()), ErrorInfo()), HttpStatus.OK))
    }

  }

  private fun parseRecentAssignments(recentAssignments: Document): List<RecentAssignment>{
    return recentAssignments.select("gradebookScore").map { element ->
      val id = element.attr("assignmentoid")
      val name = element.attr("assignmentname")
      val course = element.attr("classname")
      val credit = element.attr("grade")
      return@map RecentAssignment(id, name, course, credit)
    }
  }
}

@JvmOverloads
fun String.between(start: String, end: String, inclusive: Boolean = false): String{
  val indexStart = indexOf(start) + if (inclusive) 0 else start.length
  val indexEnd = indexOf(end) + if (inclusive) end.length else 0
  return this.substring(indexStart, indexEnd)
}

class RecentAssignment(val id: String, val name: String, val course: String, val credit: String)
