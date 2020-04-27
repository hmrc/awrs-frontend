import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val hmrcTestVersion = "3.9.0-play-26"
  private val pegdownVersion = "1.6.0"
  private val scalaTestplusPlayVersion = "3.1.3"

  val compile = Seq(
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.3",
    "uk.gov.hmrc" %% "url-builder" % "3.3.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0", // includes the global object and error handling, as well as the FrontendController classes and some common configuration
    "uk.gov.hmrc" %% "play-partials" % "6.10.0-play-26", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "uk.gov.hmrc" %% "json-encryption" % "4.5.0-play-26",
    "com.mohiva" %% "play-html-compressor" % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.54.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.9.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.14"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.12.2" % scope,
        "org.mockito" % "mockito-core" % "3.3.3" % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
