import sbt.*
import play.sbt.PlayImport.*

private object AppDependencies {

  private val bootstrapPlayVersion = "9.9.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                   %% "http-caching-client-play-30" % "12.1.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc"                   %% "play-partials-play-30"       % "10.0.0", // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc"                   %% "domain-play-30"              % "10.0.0",
    "com.yahoo.platform.yui"        %  "yuicompressor"               % "2.4.8",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"  % "10.3.0",
    "commons-codec"                 %  "commons-codec"               % "1.18.0",
    "com.googlecode.htmlcompressor" %  "htmlcompressor"              % "1.5.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapPlayVersion % Test,
    "org.jsoup"         %  "jsoup"                   % "1.18.3"             % Test,
    "org.mockito"       %  "mockito-core"            % "5.15.2"             % Test,
    "org.scalatestplus" %% "mockito-5-12"            % "3.2.19.0"           % Test
  )

  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[ModuleID] = compile ++ test
}
