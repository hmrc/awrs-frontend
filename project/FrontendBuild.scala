import sbt._

object FrontendBuild extends Build with MicroService {

  import scala.util.Properties.envOrElse

  val appName = "awrs-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}


private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val urlBuilderVersion = "2.0.0"
  private val playHealthVersion = "2.0.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val govukTemplateVersion = "5.0.0"
  private val httpCachingClientVersion = "6.1.0"
  private val playUIVersion = "5.0.0"
  private val playPartialsVersion = "5.2.0"
  private val playAuthorisedFrontendVersion = "6.2.0"
  private val playConfigVersion = "3.0.0"
  private val playGraphiteVersion = "3.1.0"
  private val pegDownVersion = "1.6.0"
  private val jSoupVersion = "1.8.3"
  private val jSonEncryptionVersion = "3.1.0"
  private val hmrcTestVersion = "2.2.0"
  private val domainVersion = "4.0.0"
  private val metricsPlayVersion = "2.4.0_0.4.1"
  private val metricsGraphiteVersion = "3.0.2"

  val compile = Seq(
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.2",
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-graphite" % playGraphiteVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUIVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "7.10.0", // includes the global object and error handling, as well as the FrontendController classes and some common configuration
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion, // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc" %% "play-config" % playConfigVersion, // includes helper classes for retrieving Play configuration
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion, // use when your frontend requires authentication
    "uk.gov.hmrc" %% "json-encryption" % jSonEncryptionVersion,
    "com.mohiva" %% "play-html-compressor" % "0.6.3" // used to pretty print html by stripping out all the whitespaces added by the playframework
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    private val scalaTestPlusVersion = "1.5.1"
    private val scalatestVersion = "2.2.6"
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown" % "pegdown" % pegDownVersion % scope,
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
