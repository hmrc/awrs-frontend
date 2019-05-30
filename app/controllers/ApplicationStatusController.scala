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
import controllers.auth.{AwrsController, ExternalUrls, StandardAuthRetrievals}
import forms.AWRSEnums.BooleanRadioEnum
import javax.inject.Inject
import models.FormBundleStatus._
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import models._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{Save4LaterService, StatusManagementService, StatusReturnType}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.AccountUtils
import views.subtemplates.application_status._

import scala.concurrent.Future

class ApplicationStatusController @Inject()(implicit val messagesApi: MessagesApi) extends AwrsController with AccountUtils with I18nSupport {

  val signInUrl = ExternalUrls.signIn
  val save4LaterService: Save4LaterService = Save4LaterService
  val statusManagementService: StatusManagementService = StatusManagementService

  def isNewBusiness(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val err = () => throw new InternalServerException("Unexpected error when evaluating if the application is a new business")
    save4LaterService.mainStore.fetchBusinessDetails(authRetrievals) flatMap {
      case Some(data: BusinessDetails) => data.newAWBusiness.get.newAWBusiness match {
        case BooleanRadioEnum.YesString => Future.successful(true)
        case BooleanRadioEnum.NoString => Future.successful(false)
        case data@_ => err()
      }
      case _ => err()
    }
  }

  private def displayStatus(printFriendly: Boolean,
                            businessType: Option[BusinessType],
                            businessCustomerDetails: Option[BusinessCustomerDetails],
                            statusReturnType: StatusReturnType,
                            isNewBusiness: Boolean,
                            authRetrievals: StandardAuthRetrievals
                           )(implicit request: Request[AnyContent]) = {
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

    lazy val safeUseStatusInfo = (info: StatusInfoSuccessResponseType => Result) =>
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
                  safeUseStatusInfo { info =>
                    if (hasAwrs(authRetrievals.enrolments)) {
                      val awrs: String = getAwrsRefNo(authRetrievals.enrolments).toString
                      val params = ApplicationMindedToRevokeParameter(status, info, organisationName, awrs)
                      displayOk(params)
                    } else {
                      showErrorPageRaw
                    }
                  }
                case Some(NoLongerMindedToRevoke) =>
                  safeUseStatusInfo { info =>
                    if (hasAwrs(authRetrievals.enrolments)) {
                      val awrs: String = getAwrsRefNo(authRetrievals.enrolments).toString
                      val params = ApplicationNoLongerMindedToRevokeParameter(status, info, organisationName, awrs)
                      displayOk(params)
                    } else {
                      showErrorPageRaw
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
                    safeUseStatusInfo { info: StatusInfoSuccessResponseType =>
                      displayOk(ApplicationMindedToRejectedParameter(status, info, organisationName))
                    }
                  case _ => pending
                }
              case _ => pending
            }
          case Approved =>
            ifNotMindedToRevoke(
              if (hasAwrs(authRetrievals.enrolments)) {
                val awrs: String = getAwrsRefNo(authRetrievals.enrolments).toString
                val params = ApplicationApprovedParameter(status, organisationName, awrs)
                displayOk(params)
              } else {
                showErrorPageRaw
              }
            )
          case ApprovedWithConditions =>
            ifNotMindedToRevoke(
              safeUseStatusInfo { info =>
                if (hasAwrs(authRetrievals.enrolments)) {
                  val awrs: String = getAwrsRefNo(authRetrievals.enrolments).toString
                  val params = ApplicationApprovedWithConditionsParameter(status, info, organisationName, awrs)
                  displayOk(params)
                } else {
                  showErrorPageRaw
                }
              }
            )
          case Rejected =>
            safeUseStatusInfo { info =>
              val params = ApplicationRejectedParameter(status, info, organisationName)
              displayOk(params)
            }
          case Revoked =>
            safeUseStatusInfo { info =>
              val params = ApplicationRevokedParameter(status, info, organisationName)
              displayOk(params)
            }
          case RejectedUnderReviewOrAppeal =>
            safeUseStatusInfo { info =>
              val params = ApplicationRejectedReviewParameter(status, info, organisationName)
              displayOk(params)
            }
          case RevokedUnderReviewOrAppeal =>
            safeUseStatusInfo { info =>
              val params = ApplicationRevokedReviewParameter(status, info, organisationName)
              displayOk(params)
            }
          case unsupportedType@_ =>
            warn(f"Application status : unsupported - $unsupportedType")
            Redirect(controllers.routes.IndexController.showIndex())
        }
    }
  }

  def showStatus(printFriendly: Boolean, mustShow: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      authorisedAction { authRetrievals =>
        for {
          businessType <- save4LaterService.mainStore.fetchBusinessType(authRetrievals)
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
          statusReturnType <- statusManagementService.retrieveStatus(authRetrievals)
          isNewBusiness <- isNewBusiness(authRetrievals)
        } yield {
          debug(s"\nshowStatus\nbusinessType : ${businessType.isDefined}\nbusinessDetails : ${businessCustomerDetails.isDefined}\nsubscriptionStatus.isDefined : ${statusReturnType.status.isDefined}\nstatusInfo.isDefined : ${statusReturnType.info.isDefined}\n")
          val showStatusPage = mustShow || !statusReturnType.wasViewed
          if (showStatusPage) {
            displayStatus(
              printFriendly,
              businessType,
              businessCustomerDetails,
              statusReturnType,
              isNewBusiness,
              authRetrievals
            )
          } else {
            Redirect(controllers.routes.IndexController.showIndex()) addSessionStatus statusReturnType.status addLocation
          }
        }
      }
  }
}
