/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.{AwrsFrontendAuditConnector, FrontendAuthConnector}
import controllers.auth.{AwrsController, ExternalUrls}
import forms.SurveyForm._
import models.Survey
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.Future

trait SurveyController extends AwrsController with AccountUtils {

  def showSurvey = async {
    implicit user => implicit request => Future.successful(Ok(views.html.awrs_survey(surveyForm.form)))
  }

  def optOutSurvey = async {
    implicit user => implicit request => Future.successful(Redirect(ExternalUrls.signOut))
  }

  def submitSurvey = async {
    implicit user => implicit request =>
      surveyForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.awrs_survey(formWithErrors))),
        surveyDetails => {
          audit(transactionName = "AWRS Survey", detail = surveyFormDataToMap(surveyDetails), eventType = eventTypeSuccess)
          Future.successful(Redirect(ExternalUrls.signOut))
        }
      )
  }

  private def surveyFormDataToMap(formData: Survey): Map[String, String] = {
    formData.getClass.getDeclaredFields.map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(formData) match {
          case Some(x) => x.toString
          case xs: Seq[Any] => xs.mkString(",")
          case x => x.toString
        })
    }.toMap
  }

}

object SurveyController extends SurveyController {
  override val authConnector = FrontendAuthConnector
}
