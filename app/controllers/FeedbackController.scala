/*
 * Copyright 2019 HM Revenue & Customs
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

import config.FrontendAuthConnector
import controllers.auth.{AwrsController, AwrsRegistrationRegime}
import forms.FeedbackForm._
import models.Feedback
import uk.gov.hmrc.play.frontend.auth.Actions
import utils.AccountUtils

import scala.concurrent.Future

trait FeedbackController extends AwrsController with Actions with AccountUtils {

  def showFeedback = async {
    implicit user => implicit request => Future.successful(Ok(views.html.awrs_feedback(feedbackForm.form)))
  }

  def showFeedbackThanks = async {
    implicit user => implicit request =>
      // Don't use AWRSController OK helper as we don't want to add the thank you view to the session location history
      Future.successful(OkNoLocation(views.html.awrs_feedback_thanks()))
  }

  def submitFeedback = async {
    implicit user => implicit request =>
      feedbackForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.awrs_feedback(formWithErrors))),
        feedbackDetails => {
          audit(transactionName = "AWRS Feedback", detail = feedbackFormDataToMap(feedbackDetails), eventType = eventTypeSuccess)
          Future.successful(Redirect(routes.FeedbackController.showFeedbackThanks()))
        }
      )
  }

  private def feedbackFormDataToMap(formData: Feedback): Map[String, String] = {
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

object FeedbackController extends FeedbackController {
  override val authConnector = FrontendAuthConnector
}
