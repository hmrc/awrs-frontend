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

package uk.gov.hmrc.helpers.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import controllers.routes
import models.AwrsUsers
import org.scalatest.matchers.must.Matchers
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.application.S4LStub
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import uk.gov.hmrc.http.HeaderNames

class HomeControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with S4LStub {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"
  val SessionId = s"mock-sessionid"
  val testResponse: String = Json.toJson(AwrsUsers(Nil,Nil)).toString

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

  val successResponse: JsValue = Json.parse(
    s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
  )

  def stubShowAndRedirectExternalCalls(data : Option[JsObject], keystoreStatus: Int): StubMapping = {
    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [{
               |    "key": "IR-SA",
               |    "identifiers": [{ "key": "utr", "value": "5810451" }],
               |    "state": "Activated"
               |   }],
               |  "affinityGroup": "Individual",
               |  "optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
               |  "groupIdentifier" : "GroupId"
               |}""".stripMargin
          )
      )
    )
    stubbedGet(s"""/keystore/business-customer-frontend/$SessionId""", keystoreStatus, businessCustomerDetailsString)
    stubS4LPut("5810451", "businessCustomerDetails", businessCustomerDetailsStringS4L)
    stubbedGet("/awrs/status-info/users/XE0001234567890", OK, testResponse)
    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)
    stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)

    stubS4LGet("5810451", "", None, Some(Tuple3("etmpDetails", Scenario.STARTED, "appStatus")))
    stubS4LGet("5810451", "", data, Some(Tuple3("etmpDetails", "appStatus", "noneGET")))

    stubS4LGet("5810451", "businessRegistrationDetails",
      Some(Json.parse(
        """{
          | "utr" : "5810451"
          |}""".stripMargin).as[JsObject]), Some(Tuple3("etmpDetails", "noneGET", "businessRegistration")))
  }

  "redirect to business type page" when {

    "for API4 journey where no Business Customer data is found and redirect to business customer" in {
      stubShowAndRedirectExternalCalls(None, NOT_FOUND)

      val controllerUrl = routes.HomeController.showOrRedirect(None).url

      val resp: WSResponse = await(client(controllerUrl).withHttpHeaders(
        HeaderNames.xSessionId -> s"$SessionId",
        "Csrf-Token" -> "nocheck"
      ).get())
      resp.header("Location") mustBe Some("http://localhost:9923/business-customer/awrs")
      resp.status mustBe 303
    }

    "for API4 journey where Business Customer and Registration data is found and AWRS feature flag is false" in {
      stubShowAndRedirectExternalCalls(Some(businessCustomerDetailsStringS4L), OK)

      val controllerUrl = routes.HomeController.showOrRedirect(None).url

      val resp: WSResponse = await(client(controllerUrl).withHttpHeaders(
        HeaderNames.xSessionId -> s"$SessionId",
        "Csrf-Token" -> "nocheck"
      ).get())
      resp.header("Location") mustBe Some("/alcohol-wholesale-scheme/business-type")
      resp.status mustBe 303
    }

  }
}
