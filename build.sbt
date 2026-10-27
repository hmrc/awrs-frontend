
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import scala.util.Random

val appName = "awrs-frontend"

val coursierDirectory = sys.env.get("COURSIER_REPOSITORIES")
  .map(_.replace("https:/", "https"))
  .getOrElse("https/repo1.maven.org/maven2/")

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "2.13.18"

lazy val appDependencies : Seq[ModuleID] = AppDependencies()
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;config.*;models.*;awrs.*;awrs.app.*;view_models.*;views.helpers.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val testShuffle = taskKey[Unit]("Runs test in reproducible order...")

testShuffle := Def.taskDyn {

  val logger = streams.value.log

  val seedOption =
    sys.props.get("test.shuffle.seed")
  val seed: Long = seedOption.map(_.toLong).getOrElse(scala.util.Random.nextInt())

  logger.info(s"Shuffling tests with seed $seed")

  val testClasses = (Test / definedTests).value.filter(test => test.name.contains("HomeControllerTest") || test.name.contains("BusinessTypeControllerTest"))

  Random.setSeed(seed)
  val shuffledClasses = Random.shuffle(testClasses.map(_.name)).mkString(" ")


  val shuffledTasks = Def.task  {
    (Test / testOnly).toTask(s" ${shuffledClasses}").value
  }
  Def.sequential(shuffledTasks)
}.value

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    Test / parallelExecution := false,
    Test / fork := true,
    Test / javaOptions ++= Seq(
      "-Dconfig.resource=test.application.conf",
      s"-javaagent:${csrCacheDirectory.value.getAbsolutePath}/${coursierDirectory}org/mockito/mockito-core/${AppDependencies.mockitoVersion}/mockito-core-${AppDependencies.mockitoVersion}.jar"
    ),
  )
  .settings(
    scalacOptions ++= Seq("-Wconf:src=target/.*:s", "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s", "-feature"),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )

  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;it/test;coverageReport")
