### What's this?
Purpose of this repo is to illustrate that even though documentation is a bit lacking, 
it's fairly easy to configure a Mongo-DB backed SPARQL service with [Apache Rya](http://rya.apache.org), 
re: this [conversation](https://twitter.com/bobdc/status/1220390987199021056).

### How to run (on Windows)
   1. Install [JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and set JAVA_HOME environment variable
   2. Install local [MongoDB Community Server](https://www.mongodb.com/download-center/community) OR use free 512mb version at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
   2. git clone [https://github.com/logycon/spring-boot-scala-rya-mongolab.git](https://github.com/logycon/spring-boot-scala-rya-mongolab.git).
   3. Edit *run_with_mvn.cmd* (if maven is installed) or *run_with_mvnw.cmd* file to specify *spring.data.mongodb.*** properties (no changes needed if running against local MongoDB on default port)
   4. Start by running *run_with_mvn.cmd* (if maven is installed) or *run_with_mvnw.cmd*
   5. To execute SPARQL there are several options:
        - Easiest: Use SPARQL playground app at https://logycon.github.io/sparql-playground-app/ . SPARQL endpoint for your local service
           will be http://localhost:8080/sparql
        - Use Swagger UI at http://localhost:8080/swagger-ui.html (not very friendly)
        - Use Postman (a little more friendly)
        
### How it works
There are a few examples in Rya's github repo that illustrate how to configure and query Mongo-backed Rya (e.g. [MongoRyaDirectExample.java](https://github.com/apache/rya/blob/09fc77c796e815f7d2f1039363705ecf8377fb74/extras/indexingExample/src/main/java/MongoRyaDirectExample.java)).             
    
In a nutshell, we need to implement SELECT, CONSTRUCT and EXEC operations on [RDF4J Repository](https://rdf4j.org/documentation/programming/repository/) configured with Rya [SAIL](https://rdf4j.org/documentation/sail/) implementation while complying with [W3C SPARQL protocol](https://www.w3.org/TR/sparql11-protocol/). 

Repository instantiation occurs in [RyaMongodbStore.scala](https://github.com/logycon/spring-boot-scala-rya-mongolab/blob/master/src/main/scala/com/logycon/ryastore/mongodb/RyaMongodbStore.scala) like so

```scala
@Component
class RyaMongodbStore @Autowired() (config: MongoConfig) extends RyaStore with SparqlOps {
  lazy val sailFactory: Sail = RyaSailFactory.getInstance(config)
  override lazy val repository: Repository = new SailRepository(sailFactory)
}
```
with MongoConfig created in the same file

```scala
@Component
class MongoConfig @Autowired() (
  @Value("${spring.data.mongodb.uri}") mongoURI: String,
  @Value("${rya.collectionPrefix}") collectionPrefix: String,
  @Value("${rya.dbname}") private val dbName: String
) extends MongoDBRdfConfiguration {

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
```

Using this, we can generalize SPARQL operations as we do in SparqlOps trait in [RyaStore.scala](https://github.com/logycon/spring-boot-scala-rya-mongolab/blob/master/src/main/scala/com/logycon/ryastore/RyaStore.scala).

```scala
trait SparqlOps {
  lazy val repository: Repository = null

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

  def performConstruct(sql: String, outputStream: OutputStream, accept: String): Unit = ???
  def performExec(sql: String): Unit = ???
}
```

See [code]( https://github.com/logycon/spring-boot-scala-rya-mongolab/blob/master/src/main/scala/com/logycon/ryastore/RyaStore.scala) for full implementation.

Finally, we can hook up the [controller](https://github.com/logycon/spring-boot-scala-rya-mongolab/blob/master/src/main/scala/com/logycon/controllers/SparqlController.scala) and call our SPARQL ops in response to requests/content types.
