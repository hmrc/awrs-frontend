import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "awrs-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}


private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._
  
  val compile = Seq(
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.3",
    "uk.gov.hmrc" %% "url-builder" % "3.3.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "8.4.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.41.0", // includes the global object and error handling, as well as the FrontendController classes and some common configuration
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-26", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "uk.gov.hmrc" %% "json-encryption" % "4.2.0",
    "com.mohiva" %% "play-html-compressor" % "0.6.3", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc" %% "auth-client" % "2.27.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.36.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-26"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "org.mockito" % "mockito-core" % "2.28.2" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
