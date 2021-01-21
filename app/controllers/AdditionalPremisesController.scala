/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.StandardAuthRetrievals
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessPremisesForm._
import javax.inject.Inject
import models.{AdditionalBusinessPremises, AdditionalBusinessPremisesList, Address}
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}


class AdditionalPremisesController @Inject()(val mcc: MessagesControllerComponents,
                                             val save4LaterService: Save4LaterService,
                                             val deEnrolService: DeEnrolService,
                                             val accountUtils: AccountUtils,
                                             val authConnector: DefaultAuthConnector,
                                             val auditable: Auditable,
                                             implicit val applicationConfig: ApplicationConfig,
                                             template: views.html.awrs_additional_premises) extends FrontendController(mcc) with JourneyPage with Deletable[AdditionalBusinessPremisesList, AdditionalBusinessPremises] with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  val blankAddress: Address = Address("", "", None, None, None)
  val blankPremise: AdditionalBusinessPremises = AdditionalBusinessPremises(Some("Yes"), Some(blankAddress), None)
  override val signInUrl: String = applicationConfig.signIn

  override def fetch(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[AdditionalBusinessPremisesList]] =
    save4LaterService.mainStore.fetchAdditionalBusinessPremisesList(authRetrievals)

  override def save(authRetrievals: StandardAuthRetrievals, data: AdditionalBusinessPremisesList)(implicit hc: HeaderCarrier): Future[AdditionalBusinessPremisesList] =
    save4LaterService.mainStore.saveAdditionalBusinessPremisesList(authRetrievals, data)

  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.AdditionalPremisesController.actionDelete(id)
  override val listObjToList: AdditionalBusinessPremisesList => List[AdditionalBusinessPremises] = (premises: AdditionalBusinessPremisesList) => premises.premises
  override val section: String = additionalBusinessPremisesName
  override val listToListObj: List[AdditionalBusinessPremises] => AdditionalBusinessPremisesList = (premises: List[AdditionalBusinessPremises]) => AdditionalBusinessPremisesList(premises)
  override val deleteHeadingParameter: String = "awrs.view_application.premises"
  override lazy val backCall: Call = controllers.routes.ViewApplicationController.viewSection(additionalBusinessPremisesName)
  override val addNoAnswerRecord: List[AdditionalBusinessPremises] => List[AdditionalBusinessPremises] = (_: List[models.AdditionalBusinessPremises]) => List(AdditionalBusinessPremises(Some("No"), None, None))
  override val amendHaveAnotherAnswer: (AdditionalBusinessPremises, String) => AdditionalBusinessPremises = (data: AdditionalBusinessPremises, newAnswer: String) => data.copy(addAnother = Some(newAnswer))

  def showPremisePage(id: Int, isLinearMode: Boolean, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authRetrievals =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }
        lazy val newEntryAction = (id: Int) =>
          Future.successful(Ok(template(businessPremisesForm.form, id, isNewRecord)))

        lazy val existingEntryAction = (data: AdditionalBusinessPremisesList, id: Int) =>
          Future.successful(Ok(template(businessPremisesForm.form.fill(data.premises(id - 1)), id, isNewRecord)))


        lazy val haveAnother = (data: AdditionalBusinessPremisesList) =>
          data.premises.last.addAnother.fold("")(x => x).equals(BooleanRadioEnum.YesString)

        lookup[AdditionalBusinessPremisesList, AdditionalBusinessPremises](
          fetchData = fetch(authRetrievals),
          id = id,
          toList = (premises: AdditionalBusinessPremisesList) => premises.premises
        )(
          newEntryAction = newEntryAction,
          existingEntryAction = existingEntryAction,
          haveAnother = haveAnother
        )
      }
    }
  }

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]) : Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    businessPremisesForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(template(formWithErrors, id, isNewRecord)))
      },
      businessPremisesData =>
        businessPremisesData.additionalPremises match {
          case Some("No") =>
            //Save it.
            save(authRetrievals, AdditionalBusinessPremisesList(premises = List(businessPremisesData))) flatMap (//redirecting to the right page based on session business type
              _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
          case _ =>
            saveThenRedirect[AdditionalBusinessPremisesList, AdditionalBusinessPremises](
              fetchData = fetch(authRetrievals),
              saveData = save,
              id = id,
              data = businessPremisesData,
              authRetrievals = authRetrievals
            )(
              haveAnotherAnswer = (data: AdditionalBusinessPremises) => data.addAnother.fold("")(x => x),
              amendHaveAnotherAnswer = amendHaveAnotherAnswer,
              hasSingleNoAnswer = (fetchData: AdditionalBusinessPremisesList) => fetchData.premises.head.additionalPremises.fold("")(x => x)
            )(
              listObjToList = (listObj: AdditionalBusinessPremisesList) => listObj.premises,
              listToListObj = (list: List[AdditionalBusinessPremises]) => AdditionalBusinessPremisesList(list)
            )(
              redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
            )
        }
    )
  }

}
