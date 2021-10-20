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
import forms.AWRSEnums.BooleanRadioEnum
import javax.inject.Inject
import models.FormBundleStatus._
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import models._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DeEnrolService, Save4LaterService, StatusManagementService, StatusReturnType}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.subtemplates.application_status._
import scala.language.postfixOps

import scala.concurrent.{ExecutionContext, Future}

class ApplicationStatusController @Inject()(mcc: MessagesControllerComponents,
                                            statusManagementService: StatusManagementService,
                                            val auditable: Auditable,
                                            val accountUtils: AccountUtils,
                                            val authConnector: DefaultAuthConnector,
                                            implicit val save4LaterService: Save4LaterService,
                                            val deEnrolService: DeEnrolService,
                                            implicit val applicationConfig: ApplicationConfig,
                                            template: views.html.awrs_application_status
                                           ) extends FrontendController(mcc) with AwrsController with I18nSupport {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def isNewBusiness(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    val err: () => Nothing = () => {
      logger.warn("[isNewBusiness] Unexpected error when evaluating if the application is a new business")
      throw new InternalServerException("Unexpected error when evaluating if the application is a new business")
    }

    val newBusinessAnswer: Future[Option[NewAWBusiness]] =
      save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals).flatMap {
        case opt @ Some(_) => Future.successful(opt)
        case _ => save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals).map { stData =>
          stData.flatMap(_.businessDetails.flatMap(_.newAWBusiness))
        }
      }

    newBusinessAnswer flatMap {
      case Some(data) =>
        data.invertedBeforeMarch2016Question.newAWBusiness match {
          case BooleanRadioEnum.YesString => Future.successful(Some(true))
          case BooleanRadioEnum.NoString => Future.successful(Some(false))
          case _ => err()
        }
      case _ => err()
    } recover {
      case _: Exception => None
    }
  }

  private def displayStatus(printFriendly: Boolean,
                            businessCustomerDetails: Option[BusinessCustomerDetails],
                            statusReturnType: StatusReturnType,
                            isNewBusiness: Boolean,
                            authRetrievals: StandardAuthRetrievals
                           )(implicit request: Request[AnyContent]) = {
    val subscriptionStatus = statusReturnType.status
    val alertStatus = statusReturnType.notification
    val statusInfo = statusReturnType.info

    lazy val displayOk = (params: ApplicationStatusParameter) => Ok(template(params, printFriendly)) addSessionStatus subscriptionStatus addLocation

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
                    if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
                      val awrs: String = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
                      val params = ApplicationMindedToRevokeParameter(status, info, organisationName, awrs)
                      displayOk(params)
                    } else {
                      showErrorPageRaw
                    }
                  }
                case Some(NoLongerMindedToRevoke) =>
                  safeUseStatusInfo { info =>
                    if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
                      val awrs: String = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
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
              if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
                val awrs: String = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
                val params = ApplicationApprovedParameter(status, organisationName, awrs)
                displayOk(params)
              } else {
                showErrorPageRaw
              }
            )
          case ApprovedWithConditions =>
            ifNotMindedToRevoke(
              safeUseStatusInfo { info =>
                if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
                  val awrs: String = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
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
    implicit req =>
      authorisedAction { authRetrievals =>
        for {
          businessType <- save4LaterService.mainStore.fetchBusinessType(authRetrievals)
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
          statusReturnType <- statusManagementService.retrieveStatus(authRetrievals)
          isNewBusiness <- isNewBusiness(authRetrievals)
        } yield {
          isNewBusiness match {
            case Some(newBusiness) => debug(s"\nshowStatus\nbusinessType : ${businessType.isDefined}\nbusinessDetails : ${businessCustomerDetails.isDefined}\nsubscriptionStatus.isDefined : ${statusReturnType.status.isDefined}\nstatusInfo.isDefined : ${statusReturnType.info.isDefined}\n")
              val showStatusPage = mustShow || !statusReturnType.wasViewed
              if (showStatusPage) {
                displayStatus(
                  printFriendly,
                  businessCustomerDetails,
                  statusReturnType,
                  newBusiness,
                  authRetrievals
                )
              } else {
                Redirect(controllers.routes.IndexController.showIndex()) addSessionStatus statusReturnType.status addLocation
              }
            case None => showErrorPageRaw
          }
        }
      }
  }
}
