package com.logycon.controllers

import java.io.OutputStream

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

  private def executeSparql(contentType: String, query: String, outputStream: OutputStream): Unit = {

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

    val queryType = getQueryType(query)
    try {
      queryType match {
        case "select" => {
          sparql.performSelect(query, outputStream, contentType)
        }

        case "construct" => {
          sparql.performConstruct(query, outputStream, contentType)
        }

        case "exec" => {
          sparql.performExec(query)
        }

        case _ => {
          outputStream.write(s"Invalid query ${query}".getBytes())
        }

      }
    } catch {
      case ex: Exception => {
        outputStream.write(s"Error running \n\n$query \n\nError: ${ex.getMessage}".getBytes())
      }
    }

  }

  @RequestMapping(path = Array("sparql"), method = Array(RequestMethod.GET))
  def getSparql(
    @RequestHeader(required = true, defaultValue = "application/json") accept: String,
    @RequestParam(required = true) query: String,
    resp: HttpServletResponse): ResponseEntity[Void] = {

    resp.setHeader("content-type", accept)
    executeSparql(accept, query, resp.getOutputStream)
    ResponseEntity.ok().build();
  }

  @RequestMapping(path = Array("sparql"),
    method = Array(RequestMethod.POST),
    consumes = Array("application/sparql-query"),
  )
  def postSparql(
    @RequestHeader(required = true, name = "Accept", defaultValue = "application/json") accept: String,
    @RequestBody(required = true) query: String,
    resp: HttpServletResponse): ResponseEntity[Void] = {

    resp.setHeader("content-type", accept)
    executeSparql(accept, query, resp.getOutputStream)
    ResponseEntity.ok().build();
  }

  @RequestMapping(path = Array("sparql"),
    method = Array(RequestMethod.POST),
    consumes = Array("application/x-www-form-urlencoded")
  )
  def postForm(
    @RequestHeader(required = true, name = "Accept", defaultValue = "application/json") accept: String,
    @RequestBody(required = true) query: MultiValueMap[String, String],
    resp: HttpServletResponse): ResponseEntity[Void] = {

    val q: String = query.getFirst("query")
    val resType = accept.split(",")(0)
    executeSparql(resType, q, resp.getOutputStream)
    ResponseEntity.ok().build();
  }

}
