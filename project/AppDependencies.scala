/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.*
import play.sbt.PlayImport.*

private object AppDependencies {

  private val bootstrapPlayVersion = "10.1.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                   %% "http-caching-client-play-30" % "12.2.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc"                   %% "play-partials-play-30"       % "10.1.0", // includes code for retrieving partials, e.g. the Help with this page form
    "com.yahoo.platform.yui"        %  "yuicompressor"               % "2.4.8",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"  % "12.8.0",
    "commons-codec"                 %  "commons-codec"               % "1.19.0",
    "com.googlecode.htmlcompressor" %  "htmlcompressor"              % "1.5.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapPlayVersion % Test,
    "org.jsoup"         %  "jsoup"                   % "1.21.1"             % Test,
    "org.mockito"       %  "mockito-core"            % "5.18.0"             % Test,
    "org.scalatestplus" %% "mockito-5-12"            % "3.2.19.0"           % Test,
    "uk.gov.hmrc"       %% "domain-play-30"          % "11.0.0"             % Test
  )

  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[ModuleID] = compile ++ test
}
