package com.logycon.ryastore

import java.io.OutputStream

import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}
import org.slf4j.{Logger, LoggerFactory}

trait RyaStore

object SparqlOps {
  val log: Logger = LoggerFactory.getLogger("Sparql")
}

trait SparqlOps {
  def repository: Repository

  @throws[Exception]
  def performSelect(sql: String, outputStream: OutputStream, accept: String): Unit = {

    def acceptToResultWriter(accept: String): TupleQueryResultWriter = {
      accept match {
        case "text/tab-separated-values" => new SPARQLResultsTSVWriter(outputStream)
        case "application/json" => new SPARQLResultsJSONWriter(outputStream)
        case "text/csv" => new SPARQLResultsCSVWriter(outputStream)
        case _ => new SPARQLResultsXMLWriter(outputStream)
      }
    }

    val conn = repository.getConnection
    val query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sql)
    val writer = acceptToResultWriter(accept)
    query.evaluate(writer)
    conn.close()
  }

  @throws[Exception]
  def performConstruct(sql: String, outputStream: OutputStream, accept: String): Unit = {

    def acceptToRDFFormat(accept: String): RDFFormat = {
      accept match {
        case "application/json" => RDFFormat.RDFJSON
        case "application/ntriples" => RDFFormat.NTRIPLES
        case "application/x-turtle" => RDFFormat.TURTLE
        case "text/rdf+n3" => RDFFormat.N3
        case "application/ld+json" => RDFFormat.JSONLD
        case _ => RDFFormat.RDFXML
      }
    }

    val conn = repository.getConnection
    val query = conn.prepareGraphQuery(QueryLanguage.SPARQL, sql)
    val rdfFormat = acceptToRDFFormat(accept)
    val writer = Rio.createWriter(rdfFormat, outputStream)
    query.evaluate(writer)
    conn.close()
  }

  @throws[Exception]
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
        throw err
      }
    }
  }
}
