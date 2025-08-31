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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{
  aResponse,
  post,
  postRequestedFor,
  stubFor,
  urlEqualTo,
  urlMatching,
  urlPathMatching,
  verify,
  exactly => exactlyTimes
}
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.matchers.must.Matchers
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.{Application, Logging}
import uk.gov.hmrc.helpers.application.S4LStub
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec, LogCapturing}
import uk.gov.hmrc.http.HeaderNames

import java.util.UUID

class BusinessTypeControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with S4LStub with LogCapturing with Logging {

  private val baseURI           = "/alcohol-wholesaler-register"
  private val subscriptionURI   = "/subscription/"
  private val safeId: String    = "XE0001234567890"
  private val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  private val enrolmentKey      = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"
  private val SessionId         = s"stubbed-${UUID.randomUUID}"
  private val saUtr             = "5810451"
  private val awrsRef           = "XAAW00000123456"

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.csrf.header.bypassHeaders.Csrf-Token"         -> "nocheck",
        "microservice.services.auth.port"                           -> wireMockPort,
        "microservice.services.awrs.host"                           -> "localhost",
        "microservice.services.awrs.port"                           -> wireMockPort,
        "microservice.services.awrs.protocol"                       -> "http",
        "microservice.services.tax-enrolments.host"                 -> "localhost",
        "microservice.services.tax-enrolments.port"                 -> wireMockPort,
        "microservice.services.tax-enrolments.protocol"             -> "http",
        "microservice.services.cachable.short-lived-cache.host"     -> "localhost",
        "microservice.services.cachable.short-lived-cache.port"     -> wireMockPort,
        "cachable.short-lived-cache.domain"                         -> "localhost",
        "cachable.short-lived-cache-api.domain"                     -> "localhost",
        "cachable.session-cache.domain"                             -> "localhost",
        "microservice.services.cachable.short-lived-cache-api.host" -> "localhost",
        "microservice.services.cachable.short-lived-cache-api.port" -> wireMockPort,
        "microservice.services.cachable.session-cache.host"         -> "localhost",
        "microservice.services.cachable.session-cache.port"         -> wireMockPort
      )
      .build()

  val businessCustomerDetailsString: String =
    """{
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

  val businessCustomerDetailsStringS4L: JsObject = Json
    .parse("""{
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
             |}""".stripMargin)
    .as[JsObject]

  val legalEntityStringS4L: JsObject =
    Json.parse("""{ "legalEntity": "SOP" }""").as[JsObject]

  val legalEntityStringS4L2: JsObject =
    Json.parse(
      """{
        |  "modelVersion": "1.0"
        |}""".stripMargin
    ).as[JsObject]

  val successResponse: JsValue = Json.parse(
    """{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber":"DummyRef"}"""
  )

  private def stubCommonExternalCalls(): StubMapping = {
    stubFor(
      post(urlMatching("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""
                 |{
                 |  "authorisedEnrolments": [
                 |    {
                 |      "key": "IR-SA",
                 |      "identifiers": [{ "key": "UTR", "value": "$saUtr" }],
                 |      "state": "Activated"
                 |    },
                 |    {
                 |      "key": "HMRC-AWRS-ORG",
                 |      "identifiers": [{ "key": "AWRSRefNumber", "value": "$awrsRef" }],
                 |      "state": "Activated"
                 |    }
                 |  ],
                 |  "affinityGroup": "Individual",
                 |  "optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
                 |  "groupIdentifier": "$enrolmentKey"
                 |}
                 |""".stripMargin
            )
        )
    )

    stubbedGet(s"""/keystore/business-customer-frontend/$SessionId""", OK, businessCustomerDetailsString)

    stubS4LPut(saUtr, "businessCustomerDetails", businessCustomerDetailsStringS4L)
    stubS4LPut(saUtr, "legalEntity", legalEntityStringS4L)

    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)
    stubbedPost(s"$baseURI$subscriptionURI$safeId", OK, successResponse.toString)

    stubS4LGet(saUtr, "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(("S4LCalls", STARTED, "SecondCall")))
    stubS4LGet(saUtr, "legalEntity", Some(legalEntityStringS4L), Some(("S4LCalls", "SecondCall", "ThirdCall")))
  }

  "BusinessTypeController.saveAndContinue" should {

    "redirect to Index when ETMP validation returns false and call /regime-etmp-check once" in {
      stubCommonExternalCalls()
      stubbedPost("/regime-etmp-check", NO_CONTENT, "")

      val controllerUrl = routes.BusinessTypeController.saveAndContinue().url

      val resp: WSResponse =
        await(
          client(controllerUrl)
            .withHttpHeaders(
              HeaderNames.xSessionId -> s"$SessionId",
              "Csrf-Token"           -> "nocheck",
              "Content-Type"         -> "application/x-www-form-urlencoded",
              "Cookie"               -> "PLAY_CSRF_TOKEN=nocheck"
            )
            .post(
              Map(
                "isSaAccount" -> Seq("true"),
                "legalEntity" -> Seq("SOP")
              )
            )
        )

      resp.status mustBe SEE_OTHER
      resp.header("Location") mustBe Some(controllers.routes.IndexController.showIndex.url)

      verify(exactlyTimes(1), postRequestedFor(urlEqualTo("/regime-etmp-check")))
    }

    "redirect to Application Status when ETMP validation returns true and call /regime-etmp-check once" in {
      stubCommonExternalCalls()

      stubbedPost(
        "/regime-etmp-check",
        OK,
        """{"regimeRefNumber":"XAAW00000123456"}""".stripMargin
      )

      stubbedPost(
        s"/tax-enrolments/groups/HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef/enrolments/HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef",
        CREATED,
        ""
      )

      val subscriptionTypeFrontEndCache: JsObject =
        Json.parse(
          s"""{
             |   "modelVersion": "1.0",
             |   "legalEntity": {
             |     "legalEntity": "LLP"
             |   },
             |   "products": {
             |     "mainCustomers": ["John"],
             |     "productType": ["Desk"]
             |   },
             |   "applicationDeclaration": {},
             |   "suppliers": {
             |     "suppliers": []
             |   },
             |   "partnership": {
             |     "partners": [],
             |     "modelVersion": "1.0"
             |   },
             |   "businessDetails": {
             |     "newAWBusiness": {
             |       "newAWBusiness": "no"
             |     }
             |   },
             |   "tradingActivity": {
             |     "wholesalerType": ["someSalerType"],
             |     "typeOfAlcoholOrders": ["someAlcoholType"]
             |   },
             |   "businessRegistrationDetails": {},
             |   "businessContacts": {
             |     "modelVersion": "1.1"
             |   },
             |   "placeOfBusiness": {
             |     "modelVersion": "1.0"
             |   },
             |   "additionalPremises": {
             |     "premises": [{
             |       "additionalAddress": {
             |            "addressLine1":"23 High Street",
             |            "addressLine2":"Park View",
             |            "addressLine3":"Gloucester",
             |            "addressLine4":"Gloucestershire",
             |            "postcode":"NE98 1ZZ",
             |            "addressCountry":"GB"
             |      }
             |     }]
             |   }
             | }""".stripMargin
        ).as[JsObject]

      stubFor(
        WireMock
          .get(urlPathMatching(s"/awrs/.*/$awrsRef(/.*)?"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(
                s"""{
                  | "subscriptionTypeFrontEnd": {
                  |   "modelVersion": "1.0",
                  |   "legalEntity": {
                  |     "legalEntity": "LLP"
                  |   },
                  |   "products": {
                  |     "mainCustomers": ["John"],
                  |     "productType": ["Desk"]
                  |   },
                  |   "applicationDeclaration": {},
                  |   "suppliers": {
                  |     "suppliers": []
                  |   },
                  |   "partnership": {
                  |     "partners": [],
                  |     "modelVersion": "1.0"
                  |   },
                  |   "businessDetails": {
                  |     "newAWBusiness": {
                  |       "newAWBusiness": "no"
                  |     }
                  |   },
                  |   "tradingActivity": {
                  |     "wholesalerType": ["someSalerType"],
                  |     "typeOfAlcoholOrders": ["someAlcoholType"]
                  |   },
                  |   "businessRegistrationDetails": {},
                  |   "businessContacts": {
                  |     "modelVersion": "1.1"
                  |   },
                  |   "placeOfBusiness": {
                  |     "modelVersion": "1.0"
                  |   },
                  |   "additionalPremises": {
                  |     "premises": [{
                  |       "additionalAddress": {
                  |            "addressLine1":"23 High Street",
                  |            "addressLine2":"Park View",
                  |            "addressLine3":"Gloucester",
                  |            "addressLine4":"Gloucestershire",
                  |            "postcode":"NE98 1ZZ",
                  |            "addressCountry":"GB"
                  |      }
                  |     }]
                  |   }
                  | }
                  |}""".stripMargin
              )
          )
      )

      stubS4LPut(saUtr, "subscriptionTypeFrontEnd", legalEntityStringS4L2, api = true)

      stubS4LPut(saUtr, "businessNameDetails", legalEntityStringS4L2)
//      stubS4LPut(saUtr, "tradingDetails", legalEntityStringS4L2)

      val tradingStartDetailsCache: JsObject =
        Json.parse(
          """{
            |  "newAWBusiness": "yes"
            |}""".stripMargin
        ).as[JsObject]

      stubS4LPut(saUtr, "tradingStartDetails", tradingStartDetailsCache)

//      stubS4LPut(saUtr, "businessRegistrationDetails", tradingStartDetailsCache)

//      stubS4LPut(saUtr, "placeOfBusiness", legalEntityStringS4L2)

//      stubS4LPut(saUtr, "businessContacts", legalEntityStringS4L2)

      val partnerDetailsCache: JsObject =
        Json.parse(
          """{
            |  "partners": [],
            |  "modelVersion": "1.0"
            |}""".stripMargin
        ).as[JsObject]

//      stubS4LPut(saUtr, "partnerDetails", partnerDetailsCache)

      val premisesCache: JsObject =
        Json.parse(
          """{
            |  "premises": [{
            |       "additionalAddress": {
            |            "addressLine1":"23 High Street",
            |            "addressLine2":"Park View",
            |            "addressLine3":"Gloucester",
            |            "addressLine4":"Gloucestershire",
            |            "postcode":"NE98 1ZZ",
            |            "addressCountry":"GB"
            |      }
            |     }],
            |  "modelVersion": "1.0"
            |}""".stripMargin
        ).as[JsObject]

//      stubS4LPut(saUtr, "additionalBusinessPremises", premisesCache)

      val tradingActivityCache: JsObject =
        Json.parse(
          """{
            |  "wholesalerType": ["someSalerType"],
            |  "typeOfAlcoholOrders": ["someAlcoholType"],
            |  "modelVersion": "1.0"
            |}""".stripMargin
        ).as[JsObject]

//      stubS4LPut(saUtr, "tradingActivity", tradingActivityCache)

      val productsCache: JsObject =
        Json.parse(
          """{
            |  "mainCustomers": ["John"],
            |  "productType": ["Desk"],
            |  "modelVersion": "1.0"
            |}""".stripMargin
        ).as[JsObject]

//      stubS4LPut(saUtr, "products", productsCache)

      val suppliersCache: JsObject =
        Json.parse(
          """{
            |  "suppliers": [],
            |  "modelVersion": "1.0"
            |}""".stripMargin
        ).as[JsObject]

//      stubS4LPut(saUtr, "suppliers", suppliersCache)

//      stubS4LPut(saUtr, "applicationDeclaration", suppliersCache)

      stubS4LGet(saUtr, "subscriptionTypeFrontEnd", Some(subscriptionTypeFrontEndCache), None, api = true)

      stubS4LGet(saUtr, "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(("S4LCalls", "ThirdCall", "FourthCall")))
      stubS4LGet(saUtr, "legalEntity", Some(legalEntityStringS4L), Some(("S4LCalls", "FourthCall", "FifthCall")))
      stubS4LGet(saUtr, "businessCustomerDetails", Some(businessCustomerDetailsStringS4L), Some(("S4LCalls", "FifthCall", "FifthCall")))

      val controllerUrl = routes.BusinessTypeController.saveAndContinue().url

      val resp: WSResponse =
        await(
          client(controllerUrl)
            .withHttpHeaders(
              HeaderNames.xSessionId -> s"$SessionId",
              "Csrf-Token"           -> "nocheck",
              "Content-Type"         -> "application/x-www-form-urlencoded",
              "Cookie"               -> "PLAY_CSRF_TOKEN=nocheck"
            )
            .post(
              Map(
                "isSaAccount" -> Seq("true"),
                "legalEntity" -> Seq("SOP")
              )
            )
        )

      resp.status mustBe SEE_OTHER
      resp.header("Location") mustBe Some(
        controllers.routes.ApplicationStatusController.showStatus(mustShow = false).url
      )

      verify(exactlyTimes(1), postRequestedFor(urlEqualTo("/regime-etmp-check")))
    }
  }

}
