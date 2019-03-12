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
import models.FormBundleStatus._
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import models._
import play.api.mvc.{AnyContent, Request, Result}
import services.{Save4LaterService, StatusManagementService, StatusReturnType}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import views.subtemplates.application_status._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, InternalServerException }

trait ApplicationStatusController extends AwrsController with AccountUtils {

  val save4LaterService: Save4LaterService
  val statusManagementService: StatusManagementService

  def isNewBusiness(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    val err = () => throw new InternalServerException("Unexpected error when evaluating if the application is a new business")
    save4LaterService.mainStore.fetchBusinessDetails flatMap {
      case Some(data: BusinessDetails) => data.newAWBusiness.get.newAWBusiness match {
        case BooleanRadioEnum.YesString => Future.successful(true)
        case BooleanRadioEnum.NoString => Future.successful(false)
        case data@_ => err()
      }
      case _ => err()
    }
  }


  def hasAwrsUtrIsDefined(implicit user: AuthContext) = user.principal.accounts.awrs.isDefined

  private def displayStatus(printFriendly: Boolean,
                            businessType: Option[BusinessType],
                            businessCustomerDetails: Option[BusinessCustomerDetails],
                            statusReturnType: StatusReturnType,
                            isNewBusiness: Boolean
                           )(implicit user: AuthContext, request: Request[AnyContent]) = {
    val subscriptionStatus = statusReturnType.status
    val alertStatus = statusReturnType.notification
    val statusInfo = statusReturnType.info

    lazy val displayOk = (params: ApplicationStatusParameter) => Ok(views.html.awrs_application_status(params, printFriendly)) addSessionStatus subscriptionStatus addLocation

    lazy val logStatus = (status: FormBundleStatus) => {
      info(f"Application status : $status")
      alertStatus match {
        case Some(alert) => info(f"alert : $alert")
        case _ =>
      }
    }

    lazy val safeUseStatusInfo = (info: (StatusInfoSuccessResponseType) => Result) =>
      (statusInfo match {
        case Some(response) =>
          response.response match {
            case Some(s: StatusInfoSuccessResponseType) => info(s)
            case _ => showErrorPageRaw
          }
        case _ => showErrorPageRaw
      }): Result

    subscriptionStatus match {
      case None => showErrorPageRaw
      case Some(status) =>
        val organisationName: String = businessCustomerDetails.get.businessName
        val statusType = status.formBundleStatus

        // this function is used for approved and approved with conditions
        // if they do not have the minded to revoke status then the input behaviour is used
        // otherwise the minded to reovoke behaviour is used
        lazy val ifNotMindedToRevoke = (ifNotMindedToRevokeResult: Result) =>
          alertStatus match {
            case Some(notification) =>
              notification.contactType match {
                case Some(MindedToRevoke) =>
                  safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
                    hasAwrsUtrIsDefined match {
                      case true =>
                        val awrs: String = getAwrsRefNo.toString
                        val params = ApplicationMindedToRevokeParameter(status, info, organisationName, awrs)
                        displayOk(params)
                      case false => showErrorPageRaw
                    }
                  }
                case Some(NoLongerMindedToRevoke) =>
                  safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
                    hasAwrsUtrIsDefined match {
                      case true =>
                        val awrs: String = getAwrsRefNo.toString
                        val params = ApplicationNoLongerMindedToRevokeParameter(status, info, organisationName, awrs)
                        displayOk(params)
                      case false => showErrorPageRaw
                    }
                  }
                case _ => ifNotMindedToRevokeResult
              }
            case _ => ifNotMindedToRevokeResult
          }

        logStatus(statusType)
        statusType match {
          case Pending =>

            lazy val pending: Result =
              displayOk(ApplicationPendingParameter(status, organisationName, isNewBusiness))

            alertStatus match {
              case Some(notification) =>
                notification.contactType match {
                  case Some(MindedToReject) =>
                    safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
                      displayOk(ApplicationMindedToRejectedParameter(status, info, organisationName))
                    }
                  case _ => pending
                }
              case _ => pending
            }
          case Approved =>
            ifNotMindedToRevoke(
              hasAwrsUtrIsDefined match {
                case true =>
                  val awrs: String = getAwrsRefNo.toString
                  val params = ApplicationApprovedParameter(status, organisationName, awrs)
                  displayOk(params)
                case false => showErrorPageRaw
              }
            )
          case ApprovedWithConditions =>
            ifNotMindedToRevoke(
              safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
                hasAwrsUtrIsDefined match {
                  case true =>
                    val awrs: String = getAwrsRefNo.toString
                    val params = ApplicationApprovedWithConditionsParameter(status, info, organisationName, awrs)
                    displayOk(params)
                  case false => showErrorPageRaw
                }
              }
            )
          case Rejected =>
            safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
              val params = ApplicationRejectedParameter(status, info, organisationName)
              displayOk(params)
            }
          case Revoked =>
            safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
              val params = ApplicationRevokedParameter(status, info, organisationName)
              displayOk(params)
            }
          case RejectedUnderReviewOrAppeal =>
            safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
              val params = ApplicationRejectedReviewParameter(status, info, organisationName)
              displayOk(params)
            }
          case RevokedUnderReviewOrAppeal =>
            safeUseStatusInfo { (info: StatusInfoSuccessResponseType) =>
              val params = ApplicationRevokedReviewParameter(status, info, organisationName)
              displayOk(params)
            }
          case unsupportedType@_ =>
            warn(f"Application status : unsupported - $unsupportedType")
            Redirect(controllers.routes.IndexController.showIndex())
        }
    }
  }

  def showStatus(printFriendly: Boolean, mustShow: Boolean) = async {
    implicit user => implicit request =>
      for {
        businessType <- save4LaterService.mainStore.fetchBusinessType
        businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
        statusReturnType <- statusManagementService.retrieveStatus
        isNewBusiness <- isNewBusiness
      } yield {
        debug(s"\nshowStatus\nbusinessType : ${businessType.isDefined}\nbusinessDetails : ${businessCustomerDetails.isDefined}\nsubscriptionStatus.isDefined : ${statusReturnType.status.isDefined}\nstatusInfo.isDefined : ${statusReturnType.info.isDefined}\n")
        val showStatusPage = mustShow || !statusReturnType.wasViewed
        showStatusPage match {
          case true =>
            displayStatus(
              printFriendly,
              businessType,
              businessCustomerDetails,
              statusReturnType,
              isNewBusiness
            )
          case false => println(s"\n\n\n\nBusiness Type App Status : $businessType\n\n\n\n")
            Redirect(controllers.routes.IndexController.showIndex()) addSessionStatus statusReturnType.status addLocation
        }
      }
  }

}

object ApplicationStatusController extends ApplicationStatusController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val statusManagementService = StatusManagementService
}
