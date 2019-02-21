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
import controllers.auth.AwrsController
import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessDirectorsForm._
import models.{BusinessDirector, BusinessDirectors}
import services.Save4LaterService
import controllers.util._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Call, Request, Result}
import services.DataCacheKeys._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait BusinessDirectorsController extends AwrsController
  with JourneyPage
  with Deletable[BusinessDirectors, BusinessDirector]
  with SaveAndRoutable {

  override def fetch(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessDirectors]] = save4LaterService.mainStore.fetchBusinessDirectors

  override def save(data: BusinessDirectors)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessDirectors] = save4LaterService.mainStore.saveBusinessDirectors(data)

  override val deleteFormAction = (id: Int) => controllers.routes.BusinessDirectorsController.actionDelete(id)
  override val listObjToList = (directors: BusinessDirectors) => directors.directors
  override val section: String = businessDirectorsName
  override val listToListObj = (directors: List[BusinessDirector]) => BusinessDirectors(directors)
  override val deleteHeadingParameter: String = Messages("awrs.view_application.director")
  override val backCall: Call = controllers.routes.ViewApplicationController.viewSection(businessDirectorsName)
  override val addNoAnswerRecord = (emptyList: List[models.BusinessDirector]) => emptyList // in the directors page ignores this
  override val amendHaveAnotherAnswer = (data: BusinessDirector, newAnswer: String) => data.copy(otherDirectors = Some(newAnswer))

  def showBusinessDirectors(id: Int, isLinearMode: Boolean, isNewRecord: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      lazy val newEntryAction = (id: Int) =>
        Future.successful(Ok(views.html.awrs_business_directors(businessDirectorsForm.form, id, isNewRecord)))

      lazy val existingEntryAction = (data: BusinessDirectors, id: Int) =>
        Future.successful(Ok(views.html.awrs_business_directors(businessDirectorsForm.form.fill(data.directors(id - 1)), id, isNewRecord)))

      lazy val haveAnother = (data: BusinessDirectors) =>
        data.directors match {
          case Nil => true // this is added for testing purposes,
            // it's so we can still hit the linear journey by manipulating the URL after the user has deleted all their existing directors.
            // needs to return true because otherwise it'll return page not found
          case _ => data.directors.last.otherDirectors.fold("")(x => x).equals(BooleanRadioEnum.YesString)
        }

      lookup[BusinessDirectors, BusinessDirector](
        fetchData = fetch,
        id = id,
        toList = (directors: BusinessDirectors) => directors.directors
      )(
        newEntryAction = newEntryAction,
        existingEntryAction = existingEntryAction,
        haveAnother = haveAnother
      )
  }

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    businessDirectorsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_business_directors(formWithErrors, id, isNewRecord))),
      businessDirectorsData =>
        saveThenRedirect[BusinessDirectors, BusinessDirector](
          fetchData = fetch,
          saveData = save,
          id = id,
          data = businessDirectorsData
        )(
          haveAnotherAnswer = (data: BusinessDirector) => data.otherDirectors.get,
          amendHaveAnotherAnswer = amendHaveAnotherAnswer,
          hasSingleNoAnswer = (fetchData: BusinessDirectors) => "This is not required for directors"
        )(
          listObjToList = (list: BusinessDirectors) => list.directors, // these two functions will be the same since it is already a list
          listToListObj = (list: List[BusinessDirector]) => BusinessDirectors(list)
        )(
          redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
        )
    )
  }

}

object BusinessDirectorsController extends BusinessDirectorsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
