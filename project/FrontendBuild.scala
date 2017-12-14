import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "awrs-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}


private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val frontendbootstrap = "8.6.0"
  private val domainVersion = "4.1.0"
  private val hmrcTestVersion = "2.4.0"

  private val urlBuilderVersion = "2.1.0"
  private val httpCachingClientVersion = "7.0.0"
  private val playPartialsVersion = "6.1.0"
  private val pegDownVersion = "1.6.0"
  private val jSoupVersion = "1.8.3"
  private val jSonEncryptionVersion = "3.2.0"
  private val mockitoAllVersion = "1.10.19"
  private val scalaTestPlusPlayVersion = "1.5.1"
  private val scalatestVersion = "2.2.6"

  val compile = Seq(
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.2",
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendbootstrap, // includes the global object and error handling, as well as the FrontendController classes and some common configuration
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion, // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "json-encryption" % jSonEncryptionVersion,
    "com.mohiva" %% "play-html-compressor" % "0.6.3", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc" %% "auth-client" % "2.4.0"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown" % "pegdown" % pegDownVersion % scope,
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
