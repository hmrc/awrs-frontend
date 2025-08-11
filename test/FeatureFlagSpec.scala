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

import org.scalatestplus.play.PlaySpec
import scala.io.Source
import scala.util.Using

class FeatureFlagSpec extends PlaySpec {

  "Enrolment journey feature flag should be off by default" in {
    val applicationConfFileContents = Using.resource(Source.fromFile("conf/application.conf")) { source => source.getLines().mkString("") }
    val enrolmentJourneyFlagSetToTrue = applicationConfFileContents.toLowerCase().contains("feature.enrolmentjourney = true")

    withClue("Enrolment journey feature flag should be on by default in application.conf:") {
      enrolmentJourneyFlagSetToTrue mustBe true
    }
  }
}