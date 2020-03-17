
package uk.gov.hmrc.helpers.controllers

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, postRequestedFor, stubFor, urlEqualTo, urlMatching, verify, exactly => exactlyTimes}
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import controllers.routes
import org.scalatest.MustMatchers
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.crypto.{ApplicationCrypto, CryptoWithKeysFromConfig}
import uk.gov.hmrc.crypto.json.JsonEncryptor
import uk.gov.hmrc.helpers.application.S4LStub
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.test.LogCapturing
import utils.{AWRSFeatureSwitches, FeatureSwitch}

class HomeControllerISpec extends IntegrationSpec with AuthHelpers with MustMatchers with S4LStub with LogCapturing {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"
  val SessionId = s"stubbed-${UUID.randomUUID}"

  implicit lazy val jsonCrypto: CryptoWithKeysFromConfig = new ApplicationCrypto(app.configuration.underlying).JsonCrypto
  implicit lazy val encryptionFormat: JsonEncryptor[JsObject] = new JsonEncryptor[JsObject]()

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
               |  "credentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
               |  "authProviderId": { "ggCredId": "123" },
               |  "groupIdentifier" : "GroupId"
               |}""".stripMargin
          )
      )
    )
    stubbedGet(s"""/keystore/business-customer-frontend/$SessionId""", keystoreStatus, businessCustomerDetailsString)
    stubS4LPut("5810451", "businessCustomerDetails", businessCustomerDetailsStringS4L)
    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)
    stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)

    stubS4LGet("5810451", "", None, Some("etmpDetails", Scenario.STARTED, "appStatus"))
    stubS4LGet("5810451", "", data, Some("etmpDetails", "appStatus", "noneGET"))

    stubS4LGet("5810451", "businessRegistrationDetails",
      Some(Json.parse(
        """{
          | "utr" : "5810451"
          |}""".stripMargin).as[JsObject]), Some("etmpDetails", "noneGET", "businessRegistration"))
  }



  "redirect to business type page" when {

    "for API4 journey where no Business Customer data is found and redirect to business customer" in {
      stubShowAndRedirectExternalCalls(None, NOT_FOUND)

      val controllerUrl = routes.HomeController.showOrRedirect(None).url

      val resp: WSResponse = await(client(controllerUrl).withHeaders(HeaderNames.xSessionId -> s"$SessionId").get)
      resp.header("Location") mustBe Some("http://localhost:9923/business-customer/awrs")
      resp.status mustBe 303
    }

    "for API4 journey where Business Customer and Registration data is found and AWRS feature flag is false" in {
      stubShowAndRedirectExternalCalls(Some(businessCustomerDetailsStringS4L), OK)

      val controllerUrl = routes.HomeController.showOrRedirect(None).url

      val resp: WSResponse = await(client(controllerUrl).withHeaders(HeaderNames.xSessionId -> s"$SessionId").get)
      resp.header("Location") mustBe Some("/alcohol-wholesale-scheme/business-type")
      resp.status mustBe 303
    }

  }
}
