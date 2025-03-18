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

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.StandardAuthRetrievals
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.PartnershipDetailsForm._
import javax.inject.Inject
import models._
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class BusinessPartnersController @Inject()(val mcc:MessagesControllerComponents,
                                           val save4LaterService: Save4LaterService,
                                           val deEnrolService: DeEnrolService,
                                           val authConnector: DefaultAuthConnector,
                                           val auditable: Auditable,
                                           val accountUtils: AccountUtils,
                                           implicit val applicationConfig: ApplicationConfig,
                                           template: views.html.awrs_partner_member_details) extends FrontendController(mcc) with JourneyPage with Deletable[Partners, Partner] with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  //fetch function
  override def fetch(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[Partners]] = save4LaterService.mainStore.fetchPartnerDetails(authRetrievals)

  override def save(authRetrievals: StandardAuthRetrievals, data: Partners)(implicit hc: HeaderCarrier): Future[Partners] = save4LaterService.mainStore.savePartnerDetails(authRetrievals, data)

  override val listToListObj: List[Partner] => Partners = (partnerDetails: List[Partner]) => Partners(partnerDetails)
  override val listObjToList: Partners => List[Partner] = (partnerDetails: Partners) => partnerDetails.partners
  override lazy val backCall: Call = controllers.routes.ViewApplicationController.viewSection(partnersName)
  override val section: String = partnersName
  override val deleteHeadingParameter: String = "awrs.view_application.partner"
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.BusinessPartnersController.actionDelete(id)
  override val addNoAnswerRecord: List[Partner] => List[Partner] = emptyList => emptyList
  override val amendHaveAnotherAnswer: (Partner, String) => Partner = (data: Partner, newAnswer: String) => data.copy(otherPartners = Some(newAnswer))

  def showPartnerMemberDetails(id: Int, isLinearMode: Boolean, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        lazy val newEntryAction = (id: Int) =>
          Future.successful(Ok(template(partnershipDetailsForm.form, id, isNewRecord)))

        lazy val existingEntryAction = (data: Partners, id: Int) =>
          if (data.partners.isEmpty) {
            Future.successful(Ok(template(partnershipDetailsForm.form, 1, isNewRecord)))
          } else {
            val partner = data.partners(id - 1)
            val updatedSupplier = partner.copy(partnerAddress = applicationConfig.countryCodes.getAddressWithCountry(partner.partnerAddress))
            Future.successful(Ok(template(partnershipDetailsForm.form.fill(updatedSupplier), id, isNewRecord)))
          }

        lazy val haveAnother = (data: Partners) =>
          data.partners.last.otherPartners.fold("")(x => x).equals(BooleanRadioEnum.YesString)

        lookup[Partners, Partner](
          fetchData = fetch(ar),
          id = id,
          toList = (partners: Partners) => partners.partners
        )(
          newEntryAction = newEntryAction,
          existingEntryAction = existingEntryAction,
          haveAnother = haveAnother
        )
      }
    }
  }

  def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    partnershipDetailsForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, id, isNewRecord))),
      partnerData => {
        val countryCodePartnerData = partnerData.copy(partnerAddress = applicationConfig.countryCodes.getAddressWithCountryCode(partnerData.partnerAddress))

        saveThenRedirect[Partners, Partner](
          fetchData = fetch(authRetrievals),
          saveData = save,
          id = id,
          data = countryCodePartnerData,
          authRetrievals
        )(
          haveAnotherAnswer = (data: Partner) => data.otherPartners.fold("")(x => x),
          amendHaveAnotherAnswer = amendHaveAnotherAnswer,
          hasSingleNoAnswer = (_: Partners) => "This is not required for partners"
        )(
          listObjToList = (listObj: Partners) => listObj.partners,
          listToListObj = (list: List[Partner]) => Partners(list)
        )(
          redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
        )
      }
    )
  }
}

