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

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessPremisesForm._
import models.{AdditionalBusinessPremises, AdditionalBusinessPremisesList, Address}
import services.DataCacheKeys._
import services.Save4LaterService
import controllers.util._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Call, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait AdditionalPremisesController extends AwrsController with JourneyPage with Deletable[AdditionalBusinessPremisesList, AdditionalBusinessPremises] with SaveAndRoutable {

  val blankAddress = Address("", "", None, None, None)
  val blankPremise = AdditionalBusinessPremises(Some("Yes"), Some(blankAddress), None)

  override def fetch(implicit user: AuthContext, hc: HeaderCarrier) = save4LaterService.mainStore.fetchAdditionalBusinessPremisesList

  override def save(data: AdditionalBusinessPremisesList)(implicit user: AuthContext, hc: HeaderCarrier) = save4LaterService.mainStore.saveAdditionalBusinessPremisesList(data)

  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.AdditionalPremisesController.actionDelete(id)
  override val listObjToList = (premises: AdditionalBusinessPremisesList) => premises.premises
  override val section: String = additionalBusinessPremisesName
  override val listToListObj = (premises: List[AdditionalBusinessPremises]) => AdditionalBusinessPremisesList(premises)
  override val deleteHeadingParameter: String = Messages("awrs.view_application.premises")
  override val backCall: Call = controllers.routes.ViewApplicationController.viewSection(additionalBusinessPremisesName)
  override val addNoAnswerRecord = (emptyList: List[models.AdditionalBusinessPremises]) => List(AdditionalBusinessPremises(Some("No"), None, None))
  override val amendHaveAnotherAnswer = (data: AdditionalBusinessPremises, newAnswer: String) => data.copy(addAnother = Some(newAnswer))

  def showPremisePage(id: Int, isLinearMode: Boolean, isNewRecord: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }
      lazy val newEntryAction = (id: Int) =>
        Future.successful(Ok(views.html.awrs_additional_premises(businessPremisesForm.form, id, isNewRecord)))

      lazy val existingEntryAction = (data: AdditionalBusinessPremisesList, id: Int) =>
        Future.successful(Ok(views.html.awrs_additional_premises(businessPremisesForm.form.fill(data.premises(id - 1)), id, isNewRecord)))


      lazy val haveAnother = (data: AdditionalBusinessPremisesList) =>
        data.premises.last.addAnother.fold("")(x => x).equals(BooleanRadioEnum.YesString)

      lookup[AdditionalBusinessPremisesList, AdditionalBusinessPremises](
        fetchData = fetch,
        id = id,
        toList = (premises: AdditionalBusinessPremisesList) => premises.premises
      )(
        newEntryAction = newEntryAction,
        existingEntryAction = existingEntryAction,
        haveAnother = haveAnother
      )
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent] , user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    businessPremisesForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.awrs_additional_premises(formWithErrors, id, isNewRecord)))
      },
      businessPremisesData =>
        businessPremisesData.additionalPremises match {
          case Some("No") =>
            //Save it.
            save(AdditionalBusinessPremisesList(premises = List(businessPremisesData))) flatMap {
              //redirecting to the right page based on session business type
              case _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
            }
          case _ =>
            saveThenRedirect[AdditionalBusinessPremisesList, AdditionalBusinessPremises](
              fetchData = fetch,
              saveData = save,
              id = id,
              data = businessPremisesData
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

object AdditionalPremisesController extends AdditionalPremisesController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
