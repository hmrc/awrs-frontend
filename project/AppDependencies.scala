import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,

    "uk.gov.hmrc"            %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-28" % "7.19.0",
    "uk.gov.hmrc"            %% "play-partials"              % "8.4.0-play-28", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"            %% "domain"                     % "8.3.0-play-28",
    "com.yahoo.platform.yui" %  "yuicompressor"              % "2.4.8",
    "uk.gov.hmrc"            %% "play-frontend-hmrc"         % "7.14.0-play-28",
    "com.typesafe.play"      %% "play-json-joda"             % "2.9.4",
    "commons-codec"          % "commons-codec"               % "1.16.0",
    "com.github.fkoehler"    %% "play-html-compressor"       % "2.8.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"       %% "bootstrap-test-play-28"   % "7.19.0"   % scope,
        "org.jsoup"         %  "jsoup"                    % "1.16.1"   % scope,
        "org.mockito"       %  "mockito-core"             % "5.4.0"   % scope,
        "org.scalatestplus" %% "scalatestplus-mockito"    % "1.0.0-M2" % scope,
        "commons-codec"     % "commons-codec"             % "1.16.0"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28"   % "7.19.0"                 % scope,
        "com.typesafe.play"            %% "play-test"                % PlayVersion.current      % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"            % "2.35.0"                 % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"     % "2.15.2"                 % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
