/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.AwrsController
import forms.FeedbackForm._
import javax.inject.Inject
import models.Feedback
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class FeedbackController @Inject()(mcc: MessagesControllerComponents,
                                   val authConnector: DefaultAuthConnector,
                                   val auditable: Auditable,
                                   val accountUtils: AccountUtils,
                                   implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showFeedback: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar => Future.successful(Ok(views.html.awrs_feedback(feedbackForm.form))) }
  }

  def showFeedbackThanks: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      // Don't use AWRSController OK helper as we don't want to add the thank you view to the session location history
      Future.successful(OkNoLocation(views.html.awrs_feedback_thanks()))
    }
  }

  def submitFeedback: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      feedbackForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.awrs_feedback(formWithErrors))),
        feedbackDetails => {
          audit(transactionName = "AWRS Feedback", detail = feedbackFormDataToMap(feedbackDetails), eventType = eventTypeSuccess)
          Future.successful(Redirect(routes.FeedbackController.showFeedbackThanks()))
        }
      )
    }
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
