package com.herocc.school.aspencheck.aspen.course.assignment

import com.herocc.school.aspencheck.AspenCheck
import com.herocc.school.aspencheck.aspen.AspenWebFetch
import kotlinx.coroutines.*
import org.jsoup.nodes.Element
import java.lang.NullPointerException

object AssignmentInfoFetcher {
  @JvmStatic
  fun getAdditionalInfoForAssignments(aspenWebFetch: AspenWebFetch, assignments: List<Assignment>){
    sync(aspenWebFetch, assignments)
    //partialAsync(aspenWebFetch, assignments)
    //async(aspenWebFetch, assignments)
  }

  private fun sync(aspenWebFetch: AspenWebFetch, assignments: List<Assignment>){
    val responses = assignments.mapIndexed { index, assignment ->
      if (index == 0){
        aspenWebFetch.getAssignmentPage(assignment.id).parse().toAssignmentResponse(assignments)
      } else {
        aspenWebFetch.nextAssignmentPage.parse().toAssignmentResponse(assignments)
      }
    }
    responses.forEach { response ->
      assignments.first { it.id == response.id }.updateAdditionalInfo(response.category, response.stats)
    }
    println("responses = $responses")
    println("got all responses = ${responses.map { it.id } == assignments.map { it.id }}")
    aspenWebFetch.lastAssignmentDocument = null
  }

  private fun partialAsync(aspenWebFetch: AspenWebFetch, assignments: List<Assignment>){
    val responses = assignments.mapIndexed { index, assignment ->
      if (index == 0){
        aspenWebFetch.getAssignmentPage(assignment.id)
      } else {
        aspenWebFetch.nextAssignmentPage
      }
    }
    runBlocking {
      val elements = responses.map {
        CoroutineScope(Dispatchers.IO).async { it.parse().toAssignmentResponse(assignments) }
      }
      elements.awaitAll().forEach { response ->
        assignments.first { it.id == response.id }.updateAdditionalInfo(response.category, response.stats)
      }
    }
  }

  private fun async(aspenWebFetch: AspenWebFetch, assignments: List<Assignment>){
    runBlocking {
      val responses: List<Deferred<AssignmentResponse>> = assignments.mapIndexed { index, assignment ->
          if (index == 0){
            CompletableDeferred(aspenWebFetch.getAssignmentPage(assignment.id).parse().toAssignmentResponse(assignments))
          } else {
            CoroutineScope(Dispatchers.IO).async { aspenWebFetch.nextAssignmentPage.parse().toAssignmentResponse(assignments) }
          }
      }
      responses.awaitAll().forEach { response ->
        assignments.first { it.id == response.id }.updateAdditionalInfo(response.category, response.stats)
      }
      println("responses = ${responses.awaitAll()}")
      println("got all responses = ${responses.awaitAll().map { it.id } == assignments.map { it.id }}")
    }
  }

  private data class AssignmentResponse(val id: String, val category: String, val stats: Statistics?)
  private fun Element.toAssignmentResponse(assignments: List<Assignment> = listOf()): AssignmentResponse {
    try {
      val id = this.getElementsByAttributeValue("name", "originalOid").first().attr("value")
      val category = this.getElementById("propertyValue(relGcdGctOid_gctTypeDesc)-span").text()
      AspenCheck.log.info("got id = $id (${assignments.find { it.id == id }?.name})")
      AspenCheck.log.info("got category = $category")

      val statisticsTable = this.getElementById("mainTable").getElementsByAttributeValue("width", "50%")[1]
      //sometimes teachers hide stats, and "High", "Low", "Average", and "Median" are hidden as well
      if (statisticsTable.getElementsContainingOwnText("Low").size == 0){ return AssignmentResponse(id, category, null)}

      val highElement: Element = statisticsTable.getElementsContainingOwnText("High").first()

      //AspenCheck.log.info("highElement = $highElement")
      //AspenCheck.log.info("highElement.siblings = " + highElement.siblingElements().toString())
      //AspenCheck.log.info("highElement.parent = " + highElement.parent().toString())
      val high = highElement.nextElementSibling().text()
      val low: String = statisticsTable.getElementsContainingOwnText("Low").first().nextElementSibling().text()
      val average: String = statisticsTable.getElementsContainingOwnText("Average").first().nextElementSibling().text()
      val median: String = statisticsTable.getElementsContainingOwnText("Median").first().nextElementSibling().text()
      val stats = Statistics(high, low, average, median)
      return AssignmentResponse(id, category, stats)
    }catch (e: NullPointerException){
      AspenCheck.log.info("error element = $this")
      throw e
    }
  }
}

