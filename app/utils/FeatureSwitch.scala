/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import config.ApplicationConfig
import controllers.auth.{StandardAuthRetrievals, UrlSafe}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authorisedEnrolments
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait FeatureSwitch {
  def name: String
  def enabled: Boolean
}

case class BooleanFeatureSwitch(name: String, enabled: Boolean) extends FeatureSwitch

object FeatureSwitch {

  private[utils] def getProperty(name: String)(implicit applicationConfig: ApplicationConfig): FeatureSwitch = {
    val value = sys.props.get(systemPropertyName(name))
    value match {
      case Some("true") => BooleanFeatureSwitch(name, enabled = true)
      case _ => BooleanFeatureSwitch(name, applicationConfig.feature(systemPropertyName(name)))
    }
  }

  private[utils] def setProperty(name: String, value: String)(implicit applicationConfig: ApplicationConfig): FeatureSwitch = {
    sys.props += ((systemPropertyName(name), value))
    getProperty(name)
  }

  private[utils] def systemPropertyName(name: String) = s"feature.$name"

  def enable(fs: FeatureSwitch)(implicit applicationConfig: ApplicationConfig): FeatureSwitch = setProperty(fs.name, "true")
  def disable(fs: FeatureSwitch)(implicit applicationConfig: ApplicationConfig): FeatureSwitch = setProperty(fs.name, "false")
}


class AWRSFeatureSwitches @Inject() (implicit val applicationConfig: ApplicationConfig) {
  def regimeCheck(): FeatureSwitch = FeatureSwitch.getProperty("regimeCheck")
  def enrolmentJourney(): FeatureSwitch = FeatureSwitch.getProperty("enrolmentJourney")
}
