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

package utils

import models.{BusinessType, FormBundleStatus, SubscriptionStatusType}
import models.FormBundleStatus._
import org.apache.commons.lang3.RandomStringUtils
import play.api.mvc.{AnyContent, Request, Result}
import view_models.IndexViewModel

object SessionUtil {

  implicit class SessionUtilForRequest(request: Request[AnyContent]) {
    @inline def getBusinessType: Option[String] = request.session.get(AwrsSessionKeys.sessionBusinessType)

    @inline def getBusinessName: Option[String] = request.session.get(AwrsSessionKeys.sessionBusinessName)

    @inline def getSessionStatusStr: Option[String] = request.session.get(AwrsSessionKeys.sessionStatusType)

    @inline def getSessionAwrsRefNo: Option[String] = request.session.get(AwrsSessionKeys.sessionAwrsRefNo)

    @inline def getSessionStatus: Option[FormBundleStatus] = getSessionStatusStr match {
      case Some(NoStatus.name) => Some(NoStatus)
      case Some(Pending.name) => Some(Pending)
      case Some(Approved.name) => Some(Approved)
      case Some(ApprovedWithConditions.name) => Some(ApprovedWithConditions)
      case Some(Rejected.name) => Some(Rejected)
      case Some(RejectedUnderReviewOrAppeal.name) => Some(RejectedUnderReviewOrAppeal)
      case Some(Revoked.name) => Some(Revoked)
      case Some(RevokedUnderReviewOrAppeal.name) => Some(RevokedUnderReviewOrAppeal)
      case Some(DeRegistered.name) => Some(DeRegistered)
      case Some(Withdrawal.name) => Some(Withdrawal)
      case None => None
      case code@_ => Some(models.FormBundleStatus.NotFound(code.get))
    }

    @inline def getCurrentLocation: Option[String] = request.session.get(AwrsSessionKeys.sessionCurrentLocation)

    @inline def getPreviousLocation: Option[String] = request.session.get(AwrsSessionKeys.sessionPreviousLocation)

    @inline def getJourneyStartLocation: Option[String] = request.session.get(AwrsSessionKeys.sessionJouneyStartLocation)

    @inline def getSectionStatus: Option[String] = request.session.get(AwrsSessionKeys.sessionSectionStatus)

    @inline def getProcessingDate: Option[String] = request.session.get(AwrsSessionKeys.sessionProcessingDate)
  }

  implicit class SessionUtilForResult(result: Result) {

    @inline def addBusinessTypeToSession(businessType: String)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionBusinessType -> businessType)

    @inline def addBusinessTypeToSession(businessType: Option[BusinessType])(implicit request: Request[AnyContent]): Result =
      addBusinessTypeToSession(businessType.get)

    @inline def addBusinessTypeToSession(businessType: BusinessType)(implicit request: Request[AnyContent]): Result =
      addBusinessTypeToSession(businessType.legalEntity.fold("SOP")(x => x))

    @inline def addToSession(status: FormBundleStatus)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionStatusType -> status.name)

    @inline def addAwrsRefToSession(awrsRef: String)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionAwrsRefNo -> awrsRef)

    @inline def addBusinessNameToSession(businessName: String)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionBusinessName -> businessName)

    @inline def addJouneyStartLocationToSession(startLocation: String)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionJouneyStartLocation -> startLocation)

    @inline def removeJouneyStartLocationFromSession(implicit request: Request[AnyContent]): Result =
      result.removingFromSession(AwrsSessionKeys.sessionJouneyStartLocation)

    @inline def addSectionStatusToSession(indexViewModel: IndexViewModel)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionSectionStatus -> indexViewModel.toSessionHash)

    @inline def addProcessingDateToSession(processingDate:String)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(AwrsSessionKeys.sessionProcessingDate -> processingDate)

    // This method must be called after withSession
    @inline def addSessionStatus(subscriptionStatusType: Option[SubscriptionStatusType])(implicit request: Request[AnyContent]): Result =
    subscriptionStatusType match {
      case Some(status) => addSessionStatus(status)
      case None => result
    }

    // This method must be called after withSession
    @inline def addSessionStatus(subscriptionStatusType: SubscriptionStatusType)(implicit request: Request[AnyContent]): Result =
    result addingToSession (AwrsSessionKeys.sessionStatusType -> subscriptionStatusType.formBundleStatus.name)

    // This method must be called after withSession
    @inline def addLocation(implicit request: Request[AnyContent]): Result =
    sessionUtilForRequest(request).getCurrentLocation match {
      case Some(location) if location.equals(request.uri) => result
      case Some(location) => result addingToSession (AwrsSessionKeys.sessionPreviousLocation -> location) addingToSession (AwrsSessionKeys.sessionCurrentLocation -> request.uri)
      case _ => result addingToSession (AwrsSessionKeys.sessionCurrentLocation -> request.uri)
    }

  }

  // cached implicit that can be used elsewhere
  // e.g. implicit val sessionUtil = SessionUtil.sessionUtil
  implicit val sessionUtilForRequest = (request: Request[AnyContent]) => new SessionUtil.SessionUtilForRequest(request)
  implicit val sessionUtilForResult = (result: Result) => new SessionUtil.SessionUtilForResult(result)

  def getUniqueAckNo: String = {
    val length = 32
    val nanoTime = System.nanoTime()
    val restChars = length - nanoTime.toString.length
    val randomChars = RandomStringUtils.randomAlphanumeric(restChars)
    randomChars + nanoTime
  }

}

object AwrsSessionKeys {
  val sessionBusinessName = "businessName"
  val sessionBusinessType = "businessType"
  val sessionStatusType = "status"
  val sessionAwrsRefNo = "awrsrefNo"
  val sessionProcessingDate = "processingDate"
  /*
  *  The following variables are used to keep history for the back button
  *
  *  currentLocation and previousLocation are designed for the back buttons in the feedback page, this is used to
  *  determine which page to return to since the link can be accessed from every page in the service.
  *
  *  jouneyStartLocation is desgined for the back button in the form pages during the linear journey, this is used
  *  to determine where the journey began so it can determine whether to return to the index page or the previous
  *  section in the journey
  */
  val sessionCurrentLocation = "currentLocation"
  val sessionPreviousLocation = "previousLocation"
  val sessionJouneyStartLocation = "jouneyStartLocation"

  val sessionSectionStatus = "sectionStatus"
  val sessionCallerId = "callerId"
}
