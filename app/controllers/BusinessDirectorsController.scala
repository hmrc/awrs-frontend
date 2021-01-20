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
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessDirectorsForm._
import javax.inject.Inject
import models.{BusinessDirector, BusinessDirectors}
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class BusinessDirectorsController @Inject()(val mcc: MessagesControllerComponents,
                                            val save4LaterService: Save4LaterService,
                                            val deEnrolService: DeEnrolService,
                                            val authConnector: DefaultAuthConnector,
                                            val auditable: Auditable,
                                            val accountUtils: AccountUtils,
                                            implicit val applicationConfig: ApplicationConfig,
                                            template: views.html.awrs_business_directors) extends FrontendController(mcc) with AwrsController
  with JourneyPage
  with Deletable[BusinessDirectors, BusinessDirector]
  with SaveAndRoutable
{
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  override def fetch(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessDirectors]] = save4LaterService.mainStore.fetchBusinessDirectors(authRetrievals)

  override def save(authRetrievals: StandardAuthRetrievals, data: BusinessDirectors)(implicit hc: HeaderCarrier): Future[BusinessDirectors] = save4LaterService.mainStore.saveBusinessDirectors(authRetrievals, data)

  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.BusinessDirectorsController.actionDelete(id)
  override val listObjToList: BusinessDirectors => List[BusinessDirector] = (directors: BusinessDirectors) => directors.directors
  override val section: String = businessDirectorsName
  override val listToListObj: List[BusinessDirector] => BusinessDirectors = (directors: List[BusinessDirector]) => BusinessDirectors(directors)
  override val deleteHeadingParameter: String = "awrs.view_application.director"
  override lazy val backCall: Call = controllers.routes.ViewApplicationController.viewSection(businessDirectorsName)
  override val addNoAnswerRecord: List[BusinessDirector] => List[BusinessDirector] = (emptyList: List[models.BusinessDirector]) => emptyList // in the directors page ignores this
  override val amendHaveAnotherAnswer: (BusinessDirector, String) => BusinessDirector = (data: BusinessDirector, newAnswer: String) => data.copy(otherDirectors = Some(newAnswer))

  def showBusinessDirectors(id: Int, isLinearMode: Boolean, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        lazy val newEntryAction = (id: Int) =>
          Future.successful(Ok(template(businessDirectorsForm.form, id, isNewRecord)))

        lazy val existingEntryAction = (data: BusinessDirectors, id: Int) =>
          Future.successful(Ok(template(businessDirectorsForm.form.fill(data.directors(id - 1)), id, isNewRecord)))

        lazy val haveAnother = (data: BusinessDirectors) =>
          data.directors match {
            case Nil => true // this is added for testing purposes,
            // it's so we can still hit the linear journey by manipulating the URL after the user has deleted all their existing directors.
            // needs to return true because otherwise it'll return page not found
            case _ => data.directors.last.otherDirectors.fold("")(x => x).equals(BooleanRadioEnum.YesString)
          }

        lookup[BusinessDirectors, BusinessDirector](
          fetchData = fetch(ar),
          id = id,
          toList = (directors: BusinessDirectors) => directors.directors
        )(
          newEntryAction = newEntryAction,
          existingEntryAction = existingEntryAction,
          haveAnother = haveAnother
        )
      }
    }
  }

   override def save(id: Int,
                     redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
                     viewApplicationType: ViewApplicationType,
                     isNewRecord: Boolean,
                     authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] = {

    implicit val viewMode: ViewApplicationType = viewApplicationType
    businessDirectorsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, id, isNewRecord))),
      businessDirectorsData =>
        saveThenRedirect[BusinessDirectors, BusinessDirector](
          fetchData = fetch(authRetrievals),
          saveData = save,
          id = id,
          data = businessDirectorsData,
          authRetrievals
        )(
          haveAnotherAnswer = (data: BusinessDirector) => data.otherDirectors.get,
          amendHaveAnotherAnswer = amendHaveAnotherAnswer,
          hasSingleNoAnswer = (_: BusinessDirectors) => "This is not required for directors"
        )(
          listObjToList = (list: BusinessDirectors) => list.directors, // these two functions will be the same since it is already a list
          listToListObj = (list: List[BusinessDirector]) => BusinessDirectors(list)
        )(
          redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
        )
    )
  }

}
