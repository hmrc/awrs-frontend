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

package controllers

import java.text.SimpleDateFormat
import java.util.Calendar

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models.{FormBundleStatus, NewAWBusiness, SuccessfulSubscriptionResponse}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{when, reset}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import utils.TestUtil.{testBusinessContactsDefault, testBusinessCustomerDetails}
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}

import scala.concurrent.Future


class ConfirmationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService {

  val testConfirmationController: ConfirmationController = new ConfirmationController(
    mockMCC, testSave4LaterService, testKeyStoreService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl: String = applicationConfig.signIn
  }

  val subscribeSuccessResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")

  def validateForBothNewAndOldBusiness(test: (Boolean) => Unit): Unit = {
    Seq(true, false).foreach {
      bool =>
        beforeEach()
        setupMockKeyStoreServiceWithOnly(fetchIsNewBusiness = bool)
        test(bool)
    }
  }

  def validateForBothNewAndOldBusinessTradingDetails(test: (Boolean) => Unit): Unit = {
    Seq(true, false).foreach {
      bool =>
        beforeEach()
        setupMockKeyStoreServiceWithOnly(fetchIsNewBusiness = None)

        reset(mockMainStoreSave4LaterConnector)
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness(if (bool) "No" else "Yes", None))))
        test(bool)
    }
  }

  "Page load for Authorised users" should {

    "return a Confirmation Successful view" in {
      validateForBothNewAndOldBusiness(
        isNewBusiness =>
          getWithAuthorisedUserCt {
            result =>
              val today = Calendar.getInstance().getTime
              val dateFormat = new SimpleDateFormat("d MMMM y")
              val submissionDate = dateFormat.format(today)
              val companyName = "North East Wines"
              val uniqueRef = subscribeSuccessResponse.etmpFormBundleNumber

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("confirmation").text() should include(s"$companyName")
              document.getElementById("confirmation").text() should include(submissionDate)
              document.getElementById("print-confirmation").text() should be(Messages("awrs.generic.print_confirmation"))
              document.getElementById("print-application").text() should be(Messages("awrs.generic.application"))

              verifySave4LaterService(removeAll = 1)
              verifyApiSave4LaterService(removeAll = 0)

              isNewBusiness match {
                case true =>
                  document.getElementById(s"confirmationNoteLine1Text").text() should include(Messages(s"awrs.confirmation.newBusiness.information_what_next_0"))
                  document.getElementById(s"confirmationNoteLine2Text").text() should include(Messages(s"awrs.confirmation.newBusiness.information_what_next_1"))
                case false =>
                  document.getElementById(s"confirmationNoteLine1Text").text() should include(Messages(s"awrs.confirmation.information_what_next_0"))
                  document.getElementById(s"confirmationNoteLine2Text").text() should include (Messages(s"awrs.confirmation.information_what_next_1").replaceAll("&nbsp;"," "))
                  document.getElementById(s"confirmationNoteLine3Text").text() should include(Messages(s"awrs.confirmation.information_what_next_2"))
                  document.getElementById(s"confirmationNoteLine4Text").text() should include(Messages(s"awrs.confirmation.information_what_next_3"))
                  document.getElementById(s"confirmationNoteLine5Text").text() should include(Messages(s"awrs.confirmation.information_what_next_4"))


                  document.getElementById(s"confirmation1Text") shouldBe null
              }
              document.getElementById(s"awrsChangesQuestion") shouldBe null
          }
      )
    }

    "return a Confirmation Successful view, determining if it is a new business dynamically " in {
      validateForBothNewAndOldBusinessTradingDetails(
        isNewBusiness =>
          getWithAuthorisedUserCt {
            result =>
              val today = Calendar.getInstance().getTime
              val dateFormat = new SimpleDateFormat("d MMMM y")
              val submissionDate = dateFormat.format(today)
              val companyName = "North East Wines"
              val uniqueRef = subscribeSuccessResponse.etmpFormBundleNumber

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("confirmation").text() should include(s"$companyName")
              document.getElementById("confirmation").text() should include(submissionDate)
              document.getElementById("print-confirmation").text() should be(Messages("awrs.generic.print_confirmation"))
              document.getElementById("print-application").text() should be(Messages("awrs.generic.application"))

              verifySave4LaterService(removeAll = 1)
              verifyApiSave4LaterService(removeAll = 0)

              isNewBusiness match {
                case true =>
                  document.getElementById(s"confirmationNoteLine1Text").text() should include(Messages(s"awrs.confirmation.newBusiness.information_what_next_0"))
                  document.getElementById(s"confirmationNoteLine2Text").text() should include(Messages(s"awrs.confirmation.newBusiness.information_what_next_1"))
                case false =>
                  document.getElementById(s"confirmationNoteLine1Text").text() should include(Messages(s"awrs.confirmation.information_what_next_0"))
                  document.getElementById(s"confirmationNoteLine2Text").text() should include (Messages(s"awrs.confirmation.information_what_next_1").replaceAll("&nbsp;"," "))
                  document.getElementById(s"confirmationNoteLine3Text").text() should include(Messages(s"awrs.confirmation.information_what_next_2"))
                  document.getElementById(s"confirmationNoteLine4Text").text() should include(Messages(s"awrs.confirmation.information_what_next_3"))
                  document.getElementById(s"confirmationNoteLine5Text").text() should include(Messages(s"awrs.confirmation.information_what_next_4"))


                  document.getElementById(s"confirmation1Text") shouldBe null
              }
              document.getElementById(s"awrsChangesQuestion") shouldBe null
          }
      )
    }
  }

  "Update confirmation page " should {
    "return a Confirmation Update Successful view" in {
      val statusSet = Set(Pending, Approved, ApprovedWithConditions)
      statusSet.foreach { status =>
        validateForBothNewAndOldBusiness {
          isNewBusiness =>
            getUpdateWithAuthorisedUserCt(status) {
              result =>
                val companyName = "North East Wines"
                val uniqueRef = subscribeSuccessResponse.etmpFormBundleNumber

                val document = Jsoup.parse(contentAsString(result))

                document.getElementById("confirmation").text() should include(s"$companyName")
                val format = new SimpleDateFormat("d MMMM y")
                val resubmissionDate = format.format(Calendar.getInstance().getTime)
                document.getElementById("confirmation").text() should include(s"$resubmissionDate")
                document.getElementById("print-confirmation").text() should be(Messages("awrs.generic.print_confirmation"))
                document.getElementById("print-application").text() should be(Messages("awrs.generic.application"))

                verifySave4LaterService(removeAll = 1)
                verifyApiSave4LaterService(removeAll = 1)

                status match {
                  case Pending =>
                    document.getElementById(s"confirmationNoteLine0aText").text() should include(Messages(s"awrs.update.confirmation.pending.information_what_next_0"))
                    document.getElementById(s"confirmationNoteLine1Text").text() should include(Messages(s"awrs.update.confirmation.pending.information_what_next_1", uniqueRef))
                    document.getElementById(s"confirmationNoteLine0Text") shouldBe null
                  case _ =>
                    document.getElementById(s"confirmationNoteLine0Text").text() should include(Messages(s"awrs.update.confirmation.information_what_next_0", uniqueRef))
                    document.getElementById(s"confirmationNoteLine0aText") shouldBe null
                    document.getElementById(s"confirmationNoteLine1Text") shouldBe null
                }
                document.getElementById(s"confirmationNoteLine2Text").text() should include(Messages(s"awrs.update.confirmation.information_what_next_2"))

                document.getElementById(s"further-text-1") shouldBe null
                document.getElementById(s"further-text-2") shouldBe null
                document.getElementById(s"awrsChangesQuestion") shouldBe null
            }
        }
      }
    }
  }

  def getWithAuthorisedUserCt(test: Future[Result] => Any) {
    val request = SessionBuilder.buildRequestWithSession(userId)
    val newSession: Map[String, String] = request.session.data.+(AwrsSessionKeys.sessionAwrsRefNo -> subscribeSuccessResponse.etmpFormBundleNumber)
    val requestAmended = request.withSession(newSession.toSeq: _*)
    setAuthMocks()
    val result = testConfirmationController.showApplicationConfirmation(false, false).apply(requestAmended)
    test(result)
  }

  def getUpdateWithAuthorisedUserCt(status: FormBundleStatus)(test: Future[Result] => Any) {
    val request = SessionBuilder.buildRequestWithSession(userId)
    val newSession: Map[String, String] = request.session.data.+(AwrsSessionKeys.sessionAwrsRefNo -> subscribeSuccessResponse.etmpFormBundleNumber, AwrsSessionKeys.sessionStatusType -> status.name)
    val requestAmended = request.withSession(newSession.toSeq: _*)
    setAuthMocks()
    val result = testConfirmationController.showApplicationUpdateConfirmation(false).apply(requestAmended)
    test(result)
  }

}
