import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "com.typesafe.play"      %% "anorm"                      % "2.6.0-M1",
    "uk.gov.hmrc"            %% "http-caching-client"        % "9.6.0-play-28",
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-28" % "5.25.0",
    "uk.gov.hmrc"            %% "play-partials"              % "8.3.0-play-28", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"            %% "domain"                     % "8.1.0-play-28",
    "com.yahoo.platform.yui" %  "yuicompressor"              % "2.4.8",
    "com.mohiva"             %% "play-html-compressor"       % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "uk.gov.hmrc"            %% "play-frontend-hmrc"         % "6.3.0-play-28",
    "com.typesafe.play"      %% "play-json-joda"             % "2.9.4"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"       %% "bootstrap-test-play-28"   % "5.25.0"   % scope,
        "org.jsoup"         %  "jsoup"                    % "1.15.3"   % scope,
        "org.mockito"       %  "mockito-core"             % "4.11.0"   % scope,
        "org.scalatestplus" %% "scalatestplus-mockito"    % "1.0.0-M2" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28"   % "5.25.0"                 % scope,
        "com.typesafe.play"            %% "play-test"                % PlayVersion.current      % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"            % "2.35.0"                 % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"     % "2.14.2"                 % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
