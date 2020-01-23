package com.logycon.ryastore

import java.io.OutputStream

import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter
import org.eclipse.rdf4j.query.resultio.{AbstractQueryResultWriter, TupleQueryResultWriter}
import org.eclipse.rdf4j.repository.Repository
import org.slf4j.{LoggerFactory, Logger}

trait RyaStore {
  def repository: Repository
}

object SparqlOps {
  val log: Logger = LoggerFactory.getLogger("Sparql")
}

trait SparqlOps {
  lazy val repository: Repository = null

  private def getTupleQueryResultHandler(contentType: String, outputStream: OutputStream): AbstractQueryResultWriter with TupleQueryResultWriter = {
    contentType match {
      case "text/tab-separated-values" => new SPARQLResultsTSVWriter(outputStream)
      case "application/json" => new SPARQLResultsJSONWriter(outputStream)
      case "text/csv"=> new SPARQLResultsCSVWriter(outputStream)
      case _ => new SPARQLResultsXMLWriter(outputStream)
    }
  }

  def performSelect(sql: String, outputStream: OutputStream, contentType: String): Unit = {
    val conn = repository.getConnection
    val query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sql)
    val writer = getTupleQueryResultHandler(contentType, outputStream)
    query.evaluate(writer)
    conn.close()
  }

  def performExec(sql: String): Unit = {
    val conn = repository.getConnection
    try {
      conn.begin()
      val update = conn.prepareUpdate(QueryLanguage.SPARQL, sql)
      update.execute()
      conn.commit()
      conn.close()
    } catch {
      case err: Exception => {
        SparqlOps.log.error(s"Error executing ${sql}", err)
        conn.rollback()
        conn.close()
      }
    }
  }
}
