package com.logycon.ryastore.mongodb

import java.util

import com.logycon.ryastore.{RyaStore, SparqlOps}
import org.apache.rya.api.RdfCloudTripleStoreConfiguration
import com.mongodb.{MongoClient, MongoClientURI}
import org.apache.rya.indexing.accumulo.ConfigUtils
import org.apache.rya.mongodb.{MongoDBRdfConfiguration, MongoSecondaryIndex, StatefulMongoDBRdfConfiguration}
import org.apache.rya.sail.config.RyaSailFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.Sail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Component

object MongoConfig {
  private val log = LoggerFactory.getLogger(classOf[MongoConfig])
  val MONGO_DB_URI = "MONGO_DB_URI"
}

@Component
class MongoConfig @Autowired() (
  @Value("${spring.data.mongodb.uri}") mongoURI: String,
  @Value("${rya.collectionPrefix}") collectionPrefix: String,
  @Value("${rya.dbname}") private val dbName: String
) extends MongoDBRdfConfiguration {

  MongoConfig.log.info("MongoURI   : {}", this.mongoURI)
  MongoConfig.log.info("MongoDBName: {}", this.dbName)

  lazy val mongoClient = new MongoClient(new MongoClientURI(this.mongoURI))
  lazy val indexers: util.List[MongoSecondaryIndex] = this.getInstances("ac.additional.indexers", classOf[MongoSecondaryIndex])
  lazy val config = new StatefulMongoDBRdfConfiguration(this, this.mongoClient, indexers)

  private def init(): Unit = {
    this.setUseMock(false)
    this.set(ConfigUtils.USE_MONGO, "true")
    this.setBoolean(ConfigUtils.DISPLAY_QUERY_PLAN, false)
    this.set(MongoConfig.MONGO_DB_URI, this.mongoURI)
    this.set(MongoDBRdfConfiguration.MONGO_DB_NAME, this.dbName)
    this.set(MongoDBRdfConfiguration.MONGO_COLLECTION_PREFIX, this.collectionPrefix)
    this.setTablePrefix(this.collectionPrefix)
    this.set(RdfCloudTripleStoreConfiguration.CONF_INFER, "true")
    this.setBoolean(RdfCloudTripleStoreConfiguration.INFER_INCLUDE_SUBCLASSOF, true)
    this.setBoolean(RdfCloudTripleStoreConfiguration.INFER_INCLUDE_SAME_AS, true)
    this.setInfer(true)
  }
  init()
}

@Component
class RyaMongodbStore @Autowired() (config: MongoConfig) extends RyaStore with SparqlOps {
  lazy val sailFactory: Sail = RyaSailFactory.getInstance(config)
  override lazy val repository: Repository = new SailRepository(sailFactory)
}
