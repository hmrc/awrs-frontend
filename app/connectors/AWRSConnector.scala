/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.StandardAuthRetrievals
import exceptions.{DESValidationException, DuplicateSubscriptionException, GovernmentGatewayException, PendingDeregistrationException}
import javax.inject.Inject
import models.FormBundleStatus.Approved
import models.StatusContactType.{MindedToReject, MindedToRevoke}
import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.{AWRSFeatureSwitches, AccountUtils, LoggingUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class AWRSConnector @Inject()(http: DefaultHttpClient,
                              val auditable: Auditable,
                              val accountUtils: AccountUtils,
                              implicit val applicationConfig: ApplicationConfig) extends RawResponseReads with LoggingUtils {

  private final val subscriptionTypeJSPath = "subscriptionTypeFrontEnd"
  lazy val serviceURL: String = applicationConfig.servicesConfig.baseUrl("awrs")

  lazy val approvedInfo = ""
  val validationPattern: Regex = "(^.*submission contains one or more errors.*$)".r
  val ggFailurePattern: Regex = "(^.*government-gateway-admin.*$)".r
  val duplicateFailurePattern: Regex = "(^.*already has an active AWRS.*$)".r
  val deregFailurePattern: Regex = "(^.*whilst previous one is Under Appeal/Review or being Deregistered.*$)".r

  def submitAWRSData(fileData: JsValue, authRetrievals: StandardAuthRetrievals)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[SelfHealSubscriptionResponse, SuccessfulSubscriptionResponse]] = {

    val legalEntityType = (fileData \ subscriptionTypeJSPath \ "legalEntity" \ "legalEntity").as[String]
    val businessName = (fileData \ subscriptionTypeJSPath \ "businessCustomerDetails" \ "businessName").as[String]

    val postURL = s"""$serviceURL/awrs/send-data"""

    http.POST[JsValue, HttpResponse](postURL, fileData) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - API4 Response in Frontend ## " + response.status)
            Right(response.json.as[SuccessfulSubscriptionResponse])
          case 202 =>
            Left(response.json.as[SelfHealSubscriptionResponse])
          case 404 =>
            audit(auditAPI4TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeNotFound)
            warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - The remote endpoint has indicated that no data can be found ## ")
            info(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Request Json ## $fileData")
            throw new NotFoundException("URL not found")
          case 503 /*SERVICE_UNAVAILABLE*/ =>
            warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - WSO2 is currently experiencing problems that require live service intervention")
            info(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Request Json ## $fileData")
            throw new ServiceUnavailableException("Service unavailable")
          case 400 =>
            response.body.toString.replace("\n", "") match {
              case validationPattern(contents) =>
                audit(auditAPI4TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeBadRequest)
                warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Bad Request \n API4 Request Json From Frontend ## ")
                info(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Request Json ## $fileData")
                throw new DESValidationException("Validation against schema failed")
              case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
              case duplicateFailurePattern(contents) => throw new DuplicateSubscriptionException("This subscription already exists")
              case deregFailurePattern(contents) => throw new PendingDeregistrationException("You cannot submit a new application while your cancelled application is still pending")
              case _ => throw new BadRequestException("Bad Request: " + response.body.toString)
            }
          case 500 /*INTERNAL_SERVER_ERROR*/ =>
            audit(auditAPI4TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeInternalServerError)
            warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Unsuccessful return of data ## ")
            info(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Request Json ## $fileData")
            response.body.toString.replace("\n", "") match {
              case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
              case _ => throw new InternalServerException("Internal server error")
            }
          case status@_ =>
            audit(auditAPI4TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "status" -> status.toString, "requestJson" -> fileData.toString()), eventTypeGeneric)
            warn(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - $status Exception \n API4 Request Json From Frontend ## ")
            info(s"[$auditAPI4TxName - $businessName, $legalEntityType ] - Request Json ## $fileData")
            response.body.toString.replace("\n", "") match {
              case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
              case _ => throw new RuntimeException("Unknown response")
            }
        }
    }
  }

  def updateGroupBusinessPartner(
                                  businessName: String,
                                  legalEntityType: String,
                                  safeId: String,
                                  updateRegistrationDetailsRequest: UpdateRegistrationDetailsRequest,
                                  standardAuthRetrievals: StandardAuthRetrievals
                                )
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuccessfulUpdateGroupBusinessPartnerResponse] = {
    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val putURL = s"""$serviceURL/$awrsRefNo/registration-details/$safeId"""
    val updateRegistrationDetailsJsonRequest = Json.toJson(updateRegistrationDetailsRequest)
    http.PUT[JsValue, HttpResponse](putURL, updateRegistrationDetailsJsonRequest) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI3TxName - $businessName, $legalEntityType ] - API6 Response in frontend  ## " + response.status)
            response.json.as[SuccessfulUpdateGroupBusinessPartnerResponse]
          case 403 =>
            audit(auditAPI3TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> updateRegistrationDetailsJsonRequest.toString()), eventTypeNotFound)
            warn(s"[$auditAPI3TxName - $businessName, $legalEntityType ] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable.")
            throw new ForbiddenException(s"[$auditAPI3TxName] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable.")
          case 400 =>
            response.body.toString.replace("\n", "") match {
              case validationPattern(contents) =>
                audit(auditAPI3TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> updateRegistrationDetailsJsonRequest.toString()), eventTypeBadRequest)
                warn(s"[$auditAPI3TxName - $businessName, $legalEntityType ] - Bad Request \n API3 Request Json From Frontend ## ")
                throw new DESValidationException("Validation against schema failed")
              case _ => throw new BadRequestException(s"[$auditAPI3TxName] - The Submission has not passed validation")
            }
          case 404 =>
            audit(auditAPI3TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> updateRegistrationDetailsJsonRequest.toString()), eventTypeNotFound)
            warn(s"[$auditAPI3TxName - $businessName, $legalEntityType ] - The remote endpoint has indicated that no data can be found ## ")
            throw new NotFoundException(s"[$auditAPI3TxName] - The remote endpoint has indicated that no data can be found")
          case 500 /*INTERNAL_SERVER_ERROR*/ =>
            audit(auditAPI3TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> updateRegistrationDetailsJsonRequest.toString()), eventTypeInternalServerError)
            warn(s"[$auditAPI3TxName - $legalEntityType ] - Unsuccessful return of data")
            throw new InternalServerException(s"[$auditAPI3TxName] - WSO2 is currently experiencing problems that require live service intervention ## ")
          case 503 /*SERVICE_UNAVAILABLE*/ =>
            warn(s"[$auditAPI3TxName - $businessName, $legalEntityType ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new ServiceUnavailableException(s"[$auditAPI3TxName] - Dependant systems are currently not responding")
          case status@_ =>
            audit(auditAPI3TxName, Map("businessName" -> businessName, "legalEntityType" -> legalEntityType, "status" -> status.toString, "requestJson" -> updateRegistrationDetailsJsonRequest.toString()), eventTypeGeneric)
            warn(f"[$auditAPI3TxName - $businessName, $legalEntityType ] - $status Exception \n API6 Request Json From Frontend ## ")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def updateAWRSData(fileData: JsValue, standardAuthRetrievals: StandardAuthRetrievals)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuccessfulUpdateSubscriptionResponse] = {

    val legalEntityType = (fileData \ subscriptionTypeJSPath \ "legalEntity" \ "legalEntity").as[String]
    val businessName = (fileData \ subscriptionTypeJSPath \ "businessCustomerDetails" \ "businessName").as[String]

    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val putURL = s"""$serviceURL/awrs/update/$awrsRefNo"""
    http.PUT[JsValue, HttpResponse](putURL, fileData) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI6TxName - $businessName, $legalEntityType ] - API6 Response in frontend  ## " + response.status)
            response.json.as[SuccessfulUpdateSubscriptionResponse]
          case 404 =>
            audit(auditAPI6TxName, Map("awrsRefNo" -> awrsRefNo.toString(), "businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeNotFound)
            warn(s"[$auditAPI6TxName - $awrsRefNo, $businessName, $legalEntityType ] - The remote endpoint has indicated that no data can be found ## ")
            throw new NotFoundException(s"[$auditAPI6TxName] - The remote endpoint has indicated that no data can be found")
          case 503 /*SERVICE_UNAVAILABLE*/ =>
            warn(s"[$auditAPI6TxName - $awrsRefNo, $businessName, $legalEntityType ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new ServiceUnavailableException(s"[$auditAPI6TxName] - Dependant systems are currently not responding")
          case 400 =>
            response.body.toString.replace("\n", "") match {
              case validationPattern(contents) =>
                audit(auditAPI6TxName, Map("awrsRefNo" -> awrsRefNo.toString(), "businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeBadRequest)
                warn(s"[$auditAPI6TxName - $awrsRefNo, $businessName, $legalEntityType ] - Bad Request \n API6 Request Json From Frontend ## ")
                throw new DESValidationException("Validation against schema failed")
              case _ => throw new BadRequestException(s"[$auditAPI6TxName] - The Submission has not passed validation")
            }
          case 500 /*INTERNAL_SERVER_ERROR*/ =>
            audit(auditAPI6TxName, Map("awrsRefNo" -> awrsRefNo.toString(), "businessName" -> businessName, "legalEntityType" -> legalEntityType, "requestJson" -> fileData.toString()), eventTypeInternalServerError)
            warn(s"[$auditAPI6TxName - $awrsRefNo, $businessName, $legalEntityType ] - Unsuccessful return of data")
            throw new InternalServerException(s"[$auditAPI6TxName] - WSO2 is currently experiencing problems that require live service intervention ## ")
          case status@_ =>
            audit(auditAPI6TxName, Map("awrsRefNo" -> awrsRefNo.toString(), "businessName" -> businessName, "legalEntityType" -> legalEntityType, "status" -> status.toString, "requestJson" -> fileData.toString()), eventTypeGeneric)
            warn(f"[$auditAPI6TxName - $awrsRefNo, $businessName, $legalEntityType ] - $status Exception \n API6 Request Json From Frontend ## ")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def lookupAWRSData(standardAuthRetrievals: StandardAuthRetrievals)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {

    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val getURL = s"""$serviceURL/awrs/lookup/$awrsRefNo"""

    http.GET(getURL) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI5TxName - $awrsRefNo ] - Successful return of API5 Response")
            debug(s"[$auditAPI5TxName - $awrsRefNo ] - Json:\n${response.json}\n")
            response.json
          case 404 =>
            warn(s"[$auditAPI5TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
            throw new NotFoundException("The remote endpoint has indicated that no data can be found")
          case 503 =>
            warn(s"[$auditAPI5TxName - $awrsRefNo ] - Unsuccessful return of data")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case 400 =>
            warn(s"[$auditAPI5TxName - $awrsRefNo ] - Bad Request \n API5 Response fron DES ##" + response.body)
            throw new BadRequestException("The Submission has not passed validation")
          case 500 =>
            warn(s"[$auditAPI5TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditAPI5TxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def checkStatus(standardAuthRetrievals: StandardAuthRetrievals, orgName: String)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionStatusType] = {

    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val getURL = s"""$serviceURL/awrs/status/$awrsRefNo"""

    lazy val auditFunction = (status: FormBundleStatus) =>
      audit(transactionName = auditAPI9TxName, detail = Map("OrganisationName" -> orgName, "awrsRegistrationNumber" -> awrsRefNo, "formBundleStatus" -> status.name) ++ {
        status match {
          case Approved => Map("Message" -> approvedInfo)
          case _ => Map()
        }
      }, eventType = eventTypeSuccess)

    http.GET(getURL) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI9TxName] - Successful return of data")
            val subscriptionStatusType = response.json.as[SubscriptionStatusType](SubscriptionStatusType.reader)
            auditFunction(subscriptionStatusType.formBundleStatus)
            subscriptionStatusType
          case 404 =>
            warn(f"[$auditAPI9TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
            throw new NotFoundException("The remote endpoint has indicated that no data can be found")
          case 503 =>
            warn(f"[$auditAPI9TxName - $awrsRefNo ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case 400 =>
            warn(f"[$auditAPI9TxName - $awrsRefNo ] - The request has not passed validation")
            throw new BadRequestException("The Submission has not passed validation")
          case 500 =>
            warn(f"[$auditAPI9TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditAPI9TxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def getStatusInfo(contactNumber: String,
                    formBundleStatus: FormBundleStatus,
                    statusContactType: Option[StatusContactType],
                    authRetrievals: StandardAuthRetrievals)
                   (implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[StatusInfoType] = {
    debug("getStatusInfo")

    val awrsRefNo = accountUtils.getAwrsRefNo(authRetrievals.enrolments)

    val getURL = s"""$serviceURL/awrs/status-info/$awrsRefNo/$contactNumber"""

    lazy val auditFunction = (secureCommText: String) =>
      audit(transactionName = auditAPI11TxName, detail = Map("message-details" -> secureCommText, "companyName" -> request.session.get("businessName").getOrElse(""), "awrsRegistrationNumber" -> awrsRefNo, "formBundleStatus" -> formBundleStatus.name)
        ++ {
        statusContactType match {
          case Some(MindedToReject) => Map("notification" -> MindedToReject.name)
          case Some(MindedToRevoke) => Map("notification" -> MindedToRevoke.name)
          case _ => Map()
        }
      }, eventType = eventTypeSuccess)


    debug(f"getStatusInfo calling - $getURL")
    http.GET(getURL) map {
      response =>
        response.status match {
          case 200 =>
            val statusInfoType = response.json.as[StatusInfoType](StatusInfoType.reader)

            statusInfoType.response match {
              case Some(statusResponse) if statusResponse.isInstanceOf[StatusInfoSuccessResponseType] =>
                val secureCommText: String = statusResponse.asInstanceOf[StatusInfoSuccessResponseType].secureCommText
                auditFunction(secureCommText)
                warn("[API11] - Successful return of data")
              // No need to audit when status response is failure.
              case Some(statusResponse) if statusResponse.isInstanceOf[StatusInfoFailureResponseType] =>
                val reason: String = statusResponse.asInstanceOf[StatusInfoFailureResponseType].reason
                warn(f"[API11 - $awrsRefNo ] - Failure response returned:\n$reason")
                throw new BadRequestException(f"Failure response returned:\n$reason")
              case response@_ =>
                // this should never happen as any invalid response from etmp should be turned into a failure response by the middle service
                warn(f"[API11 - $awrsRefNo ] - Unknown response returned:\n$response")
                // given the unespected nature of this scenario, an exception is thrown for this case
                throw new BadRequestException(f"Unknown response returned:\n$response")
            }
            statusInfoType
          case 404 =>
            warn(f"[API11 - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
            throw new NotFoundException("The remote endpoint has indicated that no data can be found")
          case 503 =>
            warn(f"[API11 - $awrsRefNo ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case 400 =>
            warn(f"[API11 - $awrsRefNo ] - The request has not passed validation")
            throw new BadRequestException("The Submission has not passed validation")
          case 500 =>
            warn(f"[API11 - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[API11 - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def withdrawApplication(standardAuthRetrievals: StandardAuthRetrievals, withdrawalReason: JsValue)
                         (implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[WithdrawalResponse] = {

    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val postURL = s"""$serviceURL/awrs/withdrawal/$awrsRefNo"""
    debug(f"withdrawal calling - $postURL")

    http.POST[JsValue, HttpResponse](postURL, withdrawalReason) map {
      response =>
        response.status match {

          case 200 =>
            warn(s"[$auditAPI8TxName] - Successful return of data")
            val withdrawalResponse = response.json.as[WithdrawalResponse]
            audit(transactionName = auditAPI8TxName, detail = Map("message-details" -> withdrawalResponse.processingDate, "companyName" -> request.session.get("businessName").getOrElse(""), "awrsRegistrationNumber" -> awrsRefNo), eventType = eventTypeSuccess)
            withdrawalResponse
          case 404 =>
            warn(f"[$auditAPI8TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
            throw new NotFoundException("The remote endpoint has indicated that no data can be found")
          case 503 =>
            warn(f"[$auditAPI8TxName - $awrsRefNo ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case 400 =>
            warn(f"[$auditAPI8TxName - $awrsRefNo ] - The request has not passed validation")
            throw new BadRequestException("The Submission has not passed validation")
          case 500 =>
            warn(f"[$auditAPI8TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditAPI8TxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")

        }
    }
  }

  def deRegistration(deRegistration: DeRegistration, standardAuthRetrievals: StandardAuthRetrievals)
                    (implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[DeRegistrationType] = {
    debug("deRegistration")

    val awrsRefNo = accountUtils.getAwrsRefNo(standardAuthRetrievals.enrolments)

    val body = DeRegistration.formats.writes(deRegistration)

    val postURL = s"""$serviceURL/awrs/de-registration/$awrsRefNo"""
    debug(f"deRegistration calling - $postURL")
    http.POST(postURL, body) map {
      response =>
        response.status match {
          case 200 =>
            warn(s"[$auditAPI10TxName] - Successful return of data")
            val deRegistrationType = response.json.as[DeRegistrationType](DeRegistrationType.reader)

            deRegistrationType.response match {
              case Some(statusResponse) if statusResponse.isInstanceOf[DeRegistrationSuccessResponseType] =>
                val processingDate: String = statusResponse.asInstanceOf[DeRegistrationSuccessResponseType].processingDate
                audit(transactionName = auditAPI10TxName, detail = Map("message-details" -> processingDate, "companyName" -> request.session.get("businessName").getOrElse(""), "awrsRegistrationNumber" -> awrsRefNo), eventType = eventTypeSuccess)
              // No need to audit when status response is failure.
              case Some(statusResponse) if statusResponse.isInstanceOf[DeRegistrationFailureResponseType] =>
                val reason: String = statusResponse.asInstanceOf[DeRegistrationFailureResponseType].reason
                warn(f"[$auditAPI10TxName - $awrsRefNo ] - Failure response returned:\n$reason")
              // leaving to the higher level handlers to deal with this failure case
              case response@_ =>
                // this should never happen as any invalid response from etmp should be turned into a failure response by the middle service
                warn(f"[$auditAPI10TxName - $awrsRefNo ] - Unknown response returned:\n$response")
                // given the unespected nature of this scenario, an exception is thrown for this case
                throw new BadRequestException(f"Unknown response returned:\n$response")
            }
            deRegistrationType
          case 404 =>
            warn(f"[$auditAPI10TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
            throw new NotFoundException("The remote endpoint has indicated that no data can be found")
          case 503 =>
            warn(f"[$auditAPI10TxName - $awrsRefNo ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case 400 =>
            warn(f"[$auditAPI10TxName - $awrsRefNo ] - The request has not passed validation")
            throw new BadRequestException("The Submission has not passed validation")
          case 500 =>
            warn(f"[$auditAPI10TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditAPI10TxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
  }

  def checkEtmp(businessCustomerDetails: BusinessCustomerDetails, legalEntity: String)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SelfHealSubscriptionResponse]] = {
    if (!AWRSFeatureSwitches.regimeCheck().enabled) {
      Future.successful(None)
    } else {
      val regimeModel = CheckRegimeModel(businessCustomerDetails, legalEntity)
      val json = Json.toJson(regimeModel)
      val postURL = s"""$serviceURL/regime-etmp-check"""

      http.POST[JsValue, HttpResponse](postURL, json).map { resp =>
        resp.status match {
          case OK => Some(resp.json.as[SelfHealSubscriptionResponse])
          case _ => None
        }
      }.recover {
        case e: Exception =>
          Logger.warn(s"[AWRSConnector][checkEtmp] Etmp has returned an exception: ${e.getMessage}")
          None
      }

    }
  }
}
