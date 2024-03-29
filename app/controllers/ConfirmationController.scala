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

package controllers

import java.text.SimpleDateFormat
import java.util.Calendar

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import forms.AWRSEnums.BooleanRadioEnum
import javax.inject.Inject
import models.NewAWBusiness
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DeEnrolService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.html.awrs_application_update_confirmation
import scala.language.postfixOps

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                       save4LaterService: Save4LaterService,
                                       val keystoreService: KeyStoreService,
                                       val deEnrolService: DeEnrolService,
                                       val authConnector: DefaultAuthConnector,
                                       val auditable: Auditable,
                                       val accountUtils: AccountUtils,
                                       implicit val applicationConfig: ApplicationConfig,
                                       templateConfirmation: views.html.awrs_application_confirmation,
                                       templateUpdate: awrs_application_update_confirmation) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  // because the nature of visiting this page deletes all data in save4later
  // this method caches the result of isNewBusiness into the keystore so that when this page is loaded within the session
  // it wouldn't error out
  def isNewBusiness(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val err = () => throw new InternalServerException("Unexpected error when evaluating if the application is a new business")

    lazy val evalIsNewBusiness = save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals) flatMap {
      case Some(data: NewAWBusiness) => data.invertedBeforeMarch2016Question.newAWBusiness match {
        case BooleanRadioEnum.YesString => Future.successful(true)
        case BooleanRadioEnum.NoString => Future.successful(false)
        case _ => err()
      }
      case _ => err()
    }

    keystoreService.fetchIsNewBusiness flatMap {
      case Some(bool) => Future.successful(bool)
      case None => evalIsNewBusiness flatMap (bool => keystoreService.saveIsNewBusiness(bool) flatMap (_ => Future.successful(bool)))
    }
  }

  def showApplicationConfirmation(printFriendly: Boolean, selfHeal: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        isNewBusiness(ar) flatMap {
          isNewBusiness =>
            save4LaterService.mainStore.removeAll(ar)
            val format = new SimpleDateFormat("d MMMM y")
            val submissionDate = format.format(Calendar.getInstance().getTime)
            Future.successful(Ok(templateConfirmation(submissionDate, isNewBusiness, printFriendly, selfHeal)) addLocation)
        }
      }
    }
  }

  def showApplicationUpdateConfirmation(printFriendly: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        isNewBusiness(ar) flatMap {
          isNewBusiness =>
            save4LaterService.mainStore.removeAll(ar)
            save4LaterService.api.removeAll(ar)
            val format = new SimpleDateFormat("d MMMM y")
            val resubmissionDate = format.format(Calendar.getInstance().getTime)
            Future.successful(Ok(templateUpdate(resubmissionDate, isNewBusiness, printFriendly)) addLocation)
        }
      }
    }
  }
}
