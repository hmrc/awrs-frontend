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

package views.subtemplates.application_status

import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToReject, NoLongerMindedToRevoke}
import models.{StatusContactType, StatusInfoSuccessResponseType, SubscriptionStatusType}

sealed trait ApplicationStatusParameter {
  val status: SubscriptionStatusType

  val organisationName: String
}

sealed trait RegisteredParameter extends ApplicationStatusParameter {
  val awrsRegNo: String
}

sealed trait ExtendedInfoParameters extends ApplicationStatusParameter {
  val statusInfo: StatusInfoSuccessResponseType
}

sealed trait AlertInfoParameters extends ApplicationStatusParameter {
  val statusInfo: StatusInfoSuccessResponseType

  val alertType: StatusContactType
}

case class ApplicationPendingParameter(status: SubscriptionStatusType,
                                       organisationName: String,
                                       isNewBusiness : Boolean) extends ApplicationStatusParameter

case class ApplicationApprovedParameter(status: SubscriptionStatusType,
                                        organisationName: String,
                                        awrsRegNo: String) extends RegisteredParameter

case class ApplicationApprovedWithConditionsParameter(status: SubscriptionStatusType,
                                                      statusInfo: StatusInfoSuccessResponseType,
                                                      organisationName: String,
                                                      awrsRegNo: String) extends ExtendedInfoParameters with RegisteredParameter

case class ApplicationRejectedParameter(status: SubscriptionStatusType,
                                        statusInfo: StatusInfoSuccessResponseType,
                                        organisationName: String) extends ExtendedInfoParameters

case class ApplicationRevokedParameter(status: SubscriptionStatusType,
                                       statusInfo: StatusInfoSuccessResponseType,
                                       organisationName: String) extends ExtendedInfoParameters

case class ApplicationRejectedReviewParameter(status: SubscriptionStatusType,
                                              statusInfo: StatusInfoSuccessResponseType,
                                              organisationName: String) extends ExtendedInfoParameters

case class ApplicationRevokedReviewParameter(status: SubscriptionStatusType,
                                             statusInfo: StatusInfoSuccessResponseType,
                                             organisationName: String) extends ExtendedInfoParameters

// api 12 alert statuses
case class ApplicationMindedToRejectedParameter(status: SubscriptionStatusType,
                                                statusInfo: StatusInfoSuccessResponseType,
                                                organisationName: String) extends AlertInfoParameters {
  val alertType: StatusContactType = MindedToReject
}

case class ApplicationNoLongerMindedToRejectedParameter(status: SubscriptionStatusType,
                                                statusInfo: StatusInfoSuccessResponseType,
                                                organisationName: String) extends AlertInfoParameters {
  val alertType: StatusContactType = NoLongerMindedToReject
}

case class ApplicationMindedToRevokeParameter(status: SubscriptionStatusType,
                                              statusInfo: StatusInfoSuccessResponseType,
                                              organisationName: String,
                                              awrsRegNo: String) extends AlertInfoParameters with RegisteredParameter {
  val alertType: StatusContactType = MindedToRevoke
}

case class ApplicationNoLongerMindedToRevokeParameter(status: SubscriptionStatusType,
                                              statusInfo: StatusInfoSuccessResponseType,
                                              organisationName: String,
                                              awrsRegNo: String) extends AlertInfoParameters with RegisteredParameter {
  val alertType: StatusContactType = NoLongerMindedToRevoke
}
