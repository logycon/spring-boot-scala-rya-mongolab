package com.logycon.controllers

import com.logycon.ryastore.SparqlOps
import javax.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.{RequestBody, RequestHeader, RequestMapping, RequestMethod, RequestParam, RestController}

@RestController
@ComponentScan
class SparqlController @Autowired() (val sparql: SparqlOps) {

  private def executeSparql(contentType: String, query: String, resp: HttpServletResponse): Unit = {

    def getQueryType(query: String): String = {
      query match {
        case q if q.toLowerCase().contains("select") => "select"
        case q if q.toLowerCase().contains("construct") => "construct"
        case q if q.toLowerCase().contains("insert") => "exec"
        case q if q.toLowerCase().contains("delete") => "exec"
        case q if q.toLowerCase().contains("update") => "exec"
        case _ => "???"
      }
    }

    resp.setHeader("content-type", contentType)
    val queryType = getQueryType(query)
    try {
      queryType match {
        case "select" => {
          sparql.performSelect(query, resp.getOutputStream, contentType)
          resp.setStatus(200)
        }

        case "construct" => {
          sparql.performConstruct(query, resp.getOutputStream, contentType)
          resp.setStatus(200)
        }

        case "exec" => {
          sparql.performExec(query)
          resp.setStatus(200)
        }

        case _ => {
          val writer = resp.getWriter
          writer.write(s"Invalid query ${query}")
          writer.flush()
          resp.setStatus(500)
        }

      }
    } catch {
      case ex: Exception => {
        resp.setStatus(500)
        val writer = resp.getWriter
        writer.write(s"Error running \n\n$query \n\nError: ${ex.getMessage}")
        writer.flush()
      }
    }

  }

  @RequestMapping(path = Array("sparql"), method = Array(RequestMethod.GET))
  def getSparql(
    @RequestHeader(required = true, defaultValue = "application/json") contentType: String,
    @RequestParam(required = true) query: String,
    resp: HttpServletResponse): Unit = {
    executeSparql(contentType, query, resp)
  }

  @RequestMapping(path = Array("sparql"),
    method = Array(RequestMethod.POST),
    consumes = Array("application/sparql-query"),
  )
  def postSparql(
    @RequestHeader(required = true, name = "Accept", defaultValue = "application/json") accept: String,
    @RequestBody(required = true) query: String,
    resp: HttpServletResponse): Unit = {
    executeSparql(accept, query, resp)
  }

  @RequestMapping(path = Array("sparql"),
    method = Array(RequestMethod.POST),
    consumes = Array("application/x-www-form-urlencoded")
  )
  def postForm(
    @RequestHeader(required = true, name = "Accept", defaultValue = "application/json") accept: String,
    @RequestBody(required = true) query: MultiValueMap[String, String],
    resp: HttpServletResponse): Unit = {
    val q: String = query.getFirst("query")
    val resType = accept.split(",")(0)
    executeSparql(resType, q, resp)
  }

}
