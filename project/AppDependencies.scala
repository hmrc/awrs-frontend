import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val pegdownVersion = "1.6.0"
  private val scalaTestplusPlayVersion = "4.0.3"

  val compile = Seq(
    ws,
    "com.typesafe.play"  %% "anorm"                       % "2.6.0-M1",
    "uk.gov.hmrc"        %% "url-builder"                 % "3.5.0-play-27",
    "uk.gov.hmrc"        %% "http-caching-client"         % "9.5.0-play-27",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-27"  % "5.15.0",
    "uk.gov.hmrc"        %% "play-partials"               % "8.2.0-play-27", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"        %% "domain"                      % "5.11.0-play-27",
    "uk.gov.hmrc"        %% "json-encryption"             % "4.10.0-play-27",
    "com.mohiva"         %% "play-html-compressor"        % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc"        %% "play-frontend-hmrc"          % "1.19.0-play-27",
    "com.typesafe.play"  %% "play-json-joda"              % "2.9.2"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown"             % "pegdown"              % "1.6.0" % scope,
        "org.jsoup"               % "jsoup"                % "1.14.3" % scope,
        "org.mockito"             % "mockito-core"         % "3.12.4" % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play"  % scalaTestplusPlayVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "org.pegdown"             % "pegdown"              % pegdownVersion % scope,
        "com.typesafe.play"       %% "play-test"           % PlayVersion.current % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play"  % scalaTestplusPlayVersion % scope,
        "com.github.tomakehurst"  % "wiremock-jre8"        % "2.31.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
