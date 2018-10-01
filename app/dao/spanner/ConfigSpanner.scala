package dao.spanner

import java.io.FileInputStream

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.spanner._
import com.google.common.collect.Lists
import com.typesafe.config.ConfigFactory
import play.api.Logger


object ConfigSpanner {
  val logger: Logger = Logger(this.getClass)

  val maxSession: Int = ConfigFactory.load().getInt("dbs.spanner.maxSessions")
  val credentials: GoogleCredentials = GoogleCredentials.fromStream(new FileInputStream(ConfigFactory.load().getString("dbs.spanner.pathJsonKey")))
    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"))

  println("Credentials ::::::    "+credentials)

  val sessionsPoolOption = SessionPoolOptions
    .newBuilder()
    .setMaxSessions(maxSession)
    .setMinSessions(0)
    .setFailIfPoolExhausted()
    .build()

  val instanceID = ConfigFactory.load().getString("dbs.spanner.instanceID")
  val databaseID = ConfigFactory.load().getString("dbs.spanner.databaseID")
  val projectID = ConfigFactory.load().getString("dbs.spanner.projectID")

  val options = SpannerOptions
    .newBuilder()
    .setSessionPoolOption(sessionsPoolOption)
    .setCredentials(credentials)
    .setProjectId(projectID)
    .build()

  val spanner: Spanner = options.getService()

  val dbAdminClient: DatabaseAdminClient = spanner.getDatabaseAdminClient()

  def getConnection():DatabaseClient = {
    val sessionsPoolOption = SessionPoolOptions
      .newBuilder()
      .setMaxSessions(maxSession)
      .setMinSessions(0)
      .setFailIfPoolExhausted()
      .build()

    val options = SpannerOptions
      .newBuilder()
      .setSessionPoolOption(sessionsPoolOption)
      .setCredentials(credentials)
      .setProjectId(projectID)
      .build()

    val spanner: Spanner = options.getService()

    spanner.getDatabaseClient(
      DatabaseId.of(projectID, instanceID, databaseID)
    )
  }
}


