package com.logycon.ryastore

import java.io.OutputStream

import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.resultio.{QueryResultIO, TupleQueryResultFormat}
import org.eclipse.rdf4j.repository.Repository
import org.slf4j.{Logger, LoggerFactory}

trait RyaStore {
  def repository: Repository
}

object SparqlOps {
  val log: Logger = LoggerFactory.getLogger("Sparql")
}

trait SparqlOps {
  lazy val repository: Repository = null

  def performSelect(sql: String, outputStream: OutputStream, accept: String): Unit = {
    val conn = repository.getConnection
    val query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sql)
    val resultFormat = QueryResultIO.getParserFormatForMIMEType(accept)
    val writer = QueryResultIO.createTupleWriter(if (resultFormat.isPresent) resultFormat.get() else TupleQueryResultFormat.SPARQL, outputStream)
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
        throw err
      }
    }
  }
}
