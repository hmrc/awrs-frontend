import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val pegdownVersion = "1.6.0"
  private val scalaTestplusPlayVersion = "5.1.0"

  val compile = Seq(
    ws,
    "com.typesafe.play"  %% "anorm"                       % "2.6.0-M1",
    "uk.gov.hmrc"        %% "url-builder"                 % "3.5.0-play-28",
    "uk.gov.hmrc"        %% "http-caching-client"         % "9.5.0-play-28",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-28"  % "5.16.0",
    "uk.gov.hmrc"        %% "play-partials"               % "8.2.0-play-28", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"        %% "domain"                      % "6.2.0-play-28",
    "uk.gov.hmrc"        %% "json-encryption"             % "4.10.0-play-28",
    "com.yahoo.platform.yui" % "yuicompressor" % "2.4.8",
    "com.mohiva"         %% "play-html-compressor"        % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc"        %% "play-frontend-hmrc"          % "1.22.0-play-28",
    "com.typesafe.play"  %% "play-json-joda"              % "2.9.2"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "5.16.0",
        "org.pegdown"             % "pegdown"                 % "1.6.0" % scope,
        "org.jsoup"               % "jsoup"                   % "1.14.3" % scope,
        "org.mockito"            % "mockito-core"             % "4.0.0" % scope,
        "org.mockito"            %% "mockito-scala"           % "1.16.46" % scope,
        "org.mockito"            %% "mockito-scala-scalatest" % "1.16.46" % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play"     % scalaTestplusPlayVersion % scope,
        "org.scalatestplus"       %% "scalatestplus-mockito"        % "1.0.0-M2",
        "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % "test"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % "5.16.0",
        "org.pegdown"             % "pegdown"              % pegdownVersion % scope,
        "com.typesafe.play"       %% "play-test"           % PlayVersion.current % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play"  % scalaTestplusPlayVersion % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"           % "2.31.0",
        "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.13.0",
        "org.scalatestplus"       %% "scalatestplus-mockito"        % "1.0.0-M2",
        "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % "test"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
