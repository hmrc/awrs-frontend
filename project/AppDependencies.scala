import sbt._
import play.sbt.PlayImport._

private object AppDependencies {

  private val bootstrapPlayVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,

    "uk.gov.hmrc"                   %% "http-caching-client-play-30" % "11.2.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc"                   %% "play-partials-play-30"       % "9.1.0", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"                   %% "domain-play-30"              % "9.0.0",
    "com.yahoo.platform.yui"        %  "yuicompressor"               % "2.4.8",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"  % "8.4.0",
    "commons-codec"                 %  "commons-codec"               % "1.16.0",
    "com.googlecode.htmlcompressor" % "htmlcompressor"               % "1.5.2"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[sbt.ModuleID] = Seq(
        "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope,
        "org.jsoup"         %  "jsoup"                  % "1.17.2"             % scope,
        "org.mockito"       %  "mockito-core"           % "5.10.0"             % scope,
        "org.scalatestplus" %% "scalatestplus-mockito"  % "1.0.0-M2"           % scope,
        "commons-codec"     %  "commons-codec"          % "1.16.0"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test: Seq[sbt.ModuleID] = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope,
//        "com.typesafe.play"            %% "play-test"              % PlayVersion.current % scope,
        "org.wiremock"                 %  "wiremock"               % "3.3.1"              % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.16.1"             % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
