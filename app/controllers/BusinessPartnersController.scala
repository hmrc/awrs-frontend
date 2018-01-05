/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.PartnershipDetailsForm._
import models._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Call, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, CountryCodes}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait BusinessPartnersController extends AwrsController with JourneyPage with AccountUtils with Deletable[Partners, Partner] with SaveAndRoutable {

  //fetch function
  override def fetch(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[Partners]] = save4LaterService.mainStore.fetchPartnerDetails

  override def save(data: Partners)(implicit user: AuthContext, hc: HeaderCarrier): Future[Partners] = save4LaterService.mainStore.savePartnerDetails(data)

  override val listToListObj = (partnerDetails: List[Partner]) => Partners(partnerDetails)
  override val listObjToList = (partnerDetails: Partners) => partnerDetails.partners
  override val backCall: Call = controllers.routes.ViewApplicationController.viewSection(partnersName)
  override val section: String = partnersName
  override val deleteHeadingParameter: String = Messages("awrs.view_application.partner")
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.BusinessPartnersController.actionDelete(id)
  override val addNoAnswerRecord: (List[Partner]) => List[Partner] = (emptyList) => emptyList
  override val amendHaveAnotherAnswer = (data: Partner, newAnswer: String) => data.copy(otherPartners = Some(newAnswer))

  def showPartnerMemberDetails(id: Int, isLinearMode: Boolean, isNewRecord: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      lazy val newEntryAction = (id: Int) =>
        Future.successful(Ok(views.html.awrs_partner_member_details(partnershipDetailsForm.form, id, isNewRecord)))

      lazy val existingEntryAction = (data: Partners, id: Int) =>
        data.partners.isEmpty match {
          case true =>
            Future.successful(Ok(views.html.awrs_partner_member_details(partnershipDetailsForm.form, 1, isNewRecord)))
          case false =>
            val partner = data.partners(id - 1)
            val updatedSupplier = partner.copy(partnerAddress = CountryCodes.getAddressWithCountry(partner.partnerAddress))
            Future.successful(Ok(views.html.awrs_partner_member_details(partnershipDetailsForm.form.fill(updatedSupplier), id, isNewRecord)))
        }

      lazy val haveAnother = (data: Partners) =>
        data.partners.last.otherPartners.fold("")(x => x).equals(BooleanRadioEnum.YesString)

      lookup[Partners, Partner](
        fetchData = fetch,
        id = id,
        toList = (partners: Partners) => partners.partners
      )(
        newEntryAction = newEntryAction,
        existingEntryAction = existingEntryAction,
        haveAnother = haveAnother
      )
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    partnershipDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_partner_member_details(formWithErrors, id, isNewRecord))),
      partnerData => {
        val countryCodePartnerData = partnerData.copy(partnerAddress = CountryCodes.getAddressWithCountryCode(partnerData.partnerAddress))

        saveThenRedirect[Partners, Partner](
          fetchData = fetch,
          saveData = save,
          id = id,
          data = countryCodePartnerData
        )(
          haveAnotherAnswer = (data: Partner) => data.otherPartners.fold("")(x => x),
          amendHaveAnotherAnswer = amendHaveAnotherAnswer,
          hasSingleNoAnswer = (fetchData: Partners) => "This is not required for partners"
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

object BusinessPartnersController extends BusinessPartnersController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
