/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, postRequestedFor, stubFor, urlEqualTo, urlMatching, verify, exactly => exactlyTimes}
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.{BusinessTypeController, routes}
import models._
import org.scalatest.matchers.must.Matchers
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.{Application, Logger, Logging}
import uk.gov.hmrc.helpers.application.S4LStub
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec, JsonUtil, LogCapturing}
import uk.gov.hmrc.http.HeaderNames
import utils.{AWRSFeatureSwitches, FeatureSwitch}

import java.util.UUID

class BusinessTypeControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with S4LStub with LogCapturing with Logging {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"
  val SessionId = s"stubbed-${UUID.randomUUID}"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig(("feature.enrolmentJourney", "true")))
    .build()

  val businessCustomerDetailsString: String = """{
                                        |"id": "businessCustomerDetails",
                                        |"data": {
                                        |   "BC_Business_Details" : {
                                        |   "businessName": "String",
                                        |   "businessType": "SOP",
                                        |   "businessAddress":{
                                        |            "line_1":"23 High Street",
                                        |            "line_2":"Park View",
                                        |            "line_3":"Gloucester",
                                        |            "line_4":"Gloucestershire",
                                        |            "postcode":"NE98 1ZZ",
                                        |            "country":"GB"
                                        |         },
                                        |   "sapNumber": "1234567890",
                                        |   "safeId": "XE0001234567890",
                                        |   "isAGroup": false,
                                        |   "agentReferenceNumber": "JARN1234567",
                                        |   "firstName": "Joe",
                                        |   "lastName": "Bloggs",
                                        |   "utr": "5810451"
                                        |}
                                        |}
                                        |}""".stripMargin

  val businessCustomerDetailsStringS4L: JsObject = Json.parse("""{
                                                      |   "businessName": "String",
                                                      |   "businessType": "SOP",
                                                      |   "businessAddress":{
                                                      |            "line_1":"23 High Street",
                                                      |            "line_2":"Park View",
                                                      |            "line_3":"Gloucester",
                                                      |            "line_4":"Gloucestershire",
                                                      |            "postcode":"NE98 1ZZ",
                                                      |            "country":"GB"
                                                      |         },
                                                      |   "sapNumber": "1234567890",
                                                      |   "safeId": "XE0001234567890",
                                                      |   "isAGroup": false,
                                                      |   "agentReferenceNumber": "JARN1234567",
                                                      |   "firstName": "Joe",
                                                      |   "lastName": "Bloggs",
                                                      |   "utr": "5810451"
                                                      |}""".stripMargin).as[JsObject]

  val legalEntityStringS4L: JsObject = Json.parse("""{
                                                      |   "legalEntity": "SOP"
                                                      |}""".stripMargin).as[JsObject]

  val successResponse: JsValue = Json.parse(
    s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
  )

  val saUtr = "5810451"

  def stubShowAndRedirectExternalCalls(): StubMapping = {
    stubFor(post(urlMatching("/auth/authorise"))
      .inScenario("auth")
      .whenScenarioStateIs(STARTED)
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [{
               |    "key": "IR-SA",
               |    "identifiers": [{ "key": "UTR", "value": "$saUtr" }],
               |    "state": "Activated"
               |   }],
               |  "affinityGroup": "Individual",
               |  "optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
               |  "authProviderId": { "ggCredId": "123" },
               |  "groupIdentifier" : "GroupId"
               |}""".stripMargin
          )
      )
      .willSetStateTo("initial-auth")
    )
    stubbedGet(s"""/keystore/business-customer-frontend/$SessionId""", OK, businessCustomerDetailsString)
    stubS4LPut("5810451", "businessCustomerDetails", businessCustomerDetailsStringS4L)
    stubS4LPut("5810451", "legalEntity", legalEntityStringS4L)
    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)
    stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)

    stubS4LGet("5810451", "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(Tuple3("bt", STARTED, "legalEntity")))
  }

  "redirect to index page" when {

    "when the feature flag is off" in {
      stubShowAndRedirectExternalCalls()

      val controllerUrl = routes.BusinessTypeController.saveAndContinue().url

      val resp: WSResponse = await(client(controllerUrl).withHttpHeaders(
        HeaderNames.xSessionId -> s"$SessionId",
        "Csrf-Token" -> "nocheck"
      ).post(Map("isSaAccount" -> Seq("true"), "legalEntity" -> Seq("SOP")))
      )
      resp.header("Location") mustBe Some("/alcohol-wholesale-scheme/index")
      resp.status mustBe 303

      verify(exactlyTimes(0), postRequestedFor(urlEqualTo("/awrs/regime-etmp-check")))
    }



    "when feature flag is on, but the call fails" in {

      stubShowAndRedirectExternalCalls()
      stubbedPost("""/regime-etmp-check""", NO_CONTENT, "")

      val controllerUrl = routes.BusinessTypeController.saveAndContinue().url

      val resp: WSResponse = await(client(controllerUrl).withHttpHeaders(
        HeaderNames.xSessionId -> s"$SessionId",
        "Csrf-Token" -> "nocheck"
      ).post(Map("isSaAccount" -> Seq("true"), "legalEntity" -> Seq("SOP")))
      )
      resp.header("Location") mustBe Some("/alcohol-wholesale-scheme/index")
      resp.status mustBe 303

      verify(exactlyTimes(1), postRequestedFor(urlEqualTo("/regime-etmp-check")))
    }

  }

  "redirect to status page" when {

    "ES6 and ES8 succeed, with feature flag enabled" in {
      WireMock.resetAllScenarios()

      val authBody =
        s"""{
           |  "authorisedEnrolments": [{
           |    "key": "IR-SA",
           |    "identifiers": [{ "key": "UTR", "value": "$saUtr" }],
           |    "state": "Activated"
           |   },
           |   {
           |    "key": "HMRC-AWRS-ORG",
           |    "identifiers": [{ "key": "AWRSRefNumber", "value": "0123456" }],
           |    "state": "Activated"
           |   }
           |   ],
           |  "affinityGroup": "Individual",
           |  "optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
           |  "authProviderId": { "ggCredId": "123" },
           |  "groupIdentifier" : "GroupId"
           |}""".stripMargin

      stubShowAndRedirectExternalCalls()
      stubFor(post(urlMatching("/auth/authorise"))
        .inScenario("auth")
        .whenScenarioStateIs("initial-auth")
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(authBody)
        )
        .willSetStateTo("eacd-auth")
      )
      stubFor(post(urlMatching("/auth/authorise"))
        .inScenario("auth")
        .whenScenarioStateIs("eacd-auth")
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(authBody)
        )
        .willSetStateTo("eacd-auth-done")
      )
      stubFor(get(urlMatching(s"/awrs/lookup/0123456"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(JsonUtil.llpJson)
        )
      )

      val stfeObj = Json.toJson(Json.parse(JsonUtil.llpJson).as[AWRSFEModel].subscriptionTypeFrontEnd).as[JsObject]
      val busRegDetails = BusinessRegistrationDetails()
      val pob = PlaceOfBusiness()
      val bc = BusinessContacts()
      val partnerDetails = Partners(List())
      val newAWBusiness = NewAWBusiness("Yes", None)
      val abp = AdditionalBusinessPremisesList(List())
      val tradingAct = TradingActivity(List(), None, List(), None, None, None, None, None)
      val products = Products(List(), None, List(), None)
      val suppliers = Suppliers(List())
      val appDec = ApplicationDeclaration(None, None, None)

      stubS4LPut(saUtr, "subscriptionTypeFrontEnd", stfeObj, api = true)
      stubS4LPut(saUtr, "businessNameDetails", stfeObj)
      stubS4LPut(saUtr, "tradingStartDetails", Json.toJson(newAWBusiness).as[JsObject])
      stubS4LPut(saUtr, "businessRegistrationDetails", Json.toJson(busRegDetails).as[JsObject])
      stubS4LPut(saUtr, "placeOfBusiness", Json.toJson(pob).as[JsObject])
      stubS4LPut(saUtr, "businessContacts", Json.toJson(bc).as[JsObject])
      stubS4LPut(saUtr, "partnerDetails", Json.toJson(partnerDetails).as[JsObject])
      stubS4LPut(saUtr, "additionalBusinessPremises", Json.toJson(abp).as[JsObject])
      stubS4LPut(saUtr, "tradingActivity", Json.toJson(tradingAct).as[JsObject])
      stubS4LPut(saUtr, "products", Json.toJson(products).as[JsObject])
      stubS4LPut(saUtr, "suppliers", Json.toJson(suppliers).as[JsObject])
      stubS4LPut(saUtr, "applicationDeclaration", Json.toJson(appDec).as[JsObject])
      stubS4LGet(saUtr, "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(Tuple3("bt", STARTED, "legalEntity")))
      stubS4LGet(saUtr, "legalEntity", Some(Json.toJson(BusinessType(None, None, None)).as[JsObject]), Some(Tuple3("bt", "legalEntity", "legalEntity2")))
      stubS4LGet(saUtr, "legalEntity", Some(Json.toJson(BusinessType(None, None, None)).as[JsObject]), Some(Tuple3("bt", "legalEntity2", "secondCall")))
      stubS4LGet(saUtr, "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(Tuple3("bt", "secondCall", "end")))

      stubbedPost("""/regime-etmp-check""", OK,
        """{
          | "regimeRefNumber" : "123456"
          |}""".stripMargin)
      stubbedPost("""/tax-enrolments/groups/GroupId/enrolments/HMRC-AWRS-ORG~AWRSRefNumber~123456""".stripMargin, OK,
        """{}""".stripMargin)

      val controllerUrl = routes.BusinessTypeController.saveAndContinue().url
      withCaptureOfLoggingFrom(Logger(app.injector.instanceOf[BusinessTypeController].getClass)) { logs =>
        val resp: WSResponse = await(client(controllerUrl).withHttpHeaders(
          HeaderNames.xSessionId -> s"$SessionId",
          "Csrf-Token" -> "nocheck"
        ).post(Map(
            "isSaAccount" -> Seq("true"),
            "legalEntity" -> Seq("SOP"),
            "csrfToken" -> Seq("token")
          ))
        )
        resp.status mustBe 303
        resp.header("Location") mustBe Some("/alcohol-wholesale-scheme/status-page?mustShow=false")

        logs.exists(event => event.getMessage == "[BusinessTypeController][saveAndContinue] Upserted details and enrolments to EACD") mustBe true

        verify(exactlyTimes(1), postRequestedFor(urlEqualTo("/regime-etmp-check")))
      }
    }
  }
}
