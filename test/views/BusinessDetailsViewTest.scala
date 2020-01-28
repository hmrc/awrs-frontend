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

package views

import java.util.UUID

import builders.SessionBuilder
import controllers.BusinessDetailsController
import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessDetailsForm
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{JourneyConstants, ServicesUnitTestFixture}
import uk.gov.hmrc.auth.core.retrieve.{GGCredId, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.SessionKeys
import utils.TestUtil._
import org.mockito.Mockito._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessDetailsViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  def testRequest(extendedBusinessDetails: ExtendedBusinessDetails, entityType: String, hasAwrs: Boolean): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[ExtendedBusinessDetails](FakeRequest(), BusinessDetailsForm.businessDetailsValidationForm(entityType, hasAwrs), extendedBusinessDetails)

  val testBusinessDetailsController: BusinessDetailsController =
    new BusinessDetailsController(mockMCC, testSave4LaterService, testKeyStoreService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig) {
      override val signInUrl = "/sign-in"
    }


  override def beforeEach: Unit = {
    reset(mockAccountUtils)

    super.beforeEach()
  }

  "BusinessDetailsController" must {

    "Authorised users" must {

      "Are you a new business " should {

        "Data is valid, isNewApplication is false so we should see a message showing this" in {
          getWithAuthorisedUser(isNewApplication = false)(testRequest(testExtendedBusinessDetails(), "SOP", false)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#notNewBusiness").text shouldBe Messages("awrs.business_details.new_AWBusiness_No")
          }
        }

        "Data is valid, isNewApplication is true so we should see a message showing this" in {
          getWithAuthorisedUser(isNewApplication = true)(testRequest(testExtendedBusinessDetails(), "SOP", false)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              val info = Messages("awrs.business_details.new_AWBusiness_Yes", "10", "October", "2016")
              document.getElementById("notNewBusiness").text shouldBe info
          }
        }

        // this test are for AWRS-1413, where the proposed start date is sometimes not returned from etmp
        "proposed date is missing, isNewApplication is false, newBusiness is true then the field should be editable even after user provides the date" in {
          val info1 = Messages("awrs.business_details.newAWBusiness_information.para_1")
          val info2 = Messages("awrs.generic.date_hint")

          def ensureHaveDateIsCorrectlySetup(document: Document, haveDate: Boolean): Unit =
            haveDate match {
              case true =>
                document.getElementById("newAWBusiness.proposedStartDate-day").attr("value") should not be ""
                document.getElementById("newAWBusiness.proposedStartDate-month").attr("value") should not be ""
                document.getElementById("newAWBusiness.proposedStartDate-year").attr("value") should not be ""
              case false =>
                document.getElementById("newAWBusiness.proposedStartDate-day").attr("value") shouldBe ""
                document.getElementById("newAWBusiness.proposedStartDate-month").attr("value") shouldBe ""
                document.getElementById("newAWBusiness.proposedStartDate-year").attr("value") shouldBe ""
            }

          // the tests are ran with and without the date in the business details data fetched from save4later
          // this is to ensure page remains in edit mode even after the date has been filled in by the user
          Seq(true, false).foreach {
            haveDate =>
              getWithAuthorisedUserDataWithMissingStartDate(status = Pending, haveDate = haveDate) {
                result =>
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("newBus-information").text should include(info1)
                  document.getElementById("newBus-information").text should include(info2)
                  ensureHaveDateIsCorrectlySetup(document, haveDate)
              }
              Seq(Approved, ApprovedWithConditions).foreach {
                status =>
                  getWithAuthorisedUserDataWithMissingStartDate(status = status, haveDate = haveDate) {
                    result =>
                      val document = Jsoup.parse(contentAsString(result))
                      document.getElementById("newBus-information").text should not include info1
                      document.getElementById("newBus-information").text should include(info2)
                      ensureHaveDateIsCorrectlySetup(document, haveDate)
                  }
              }
          }
        }
      }

      allEntities.foreach {
        legalEntity =>
          s"$legalEntity" should {
            Seq(true, false).foreach {
              isLinear =>
                s"see a progress message for the isLinearJourney is set to $isLinear" in {
                  getWithAuthorisedUser(isLinearjourney = isLinear, businessType = legalEntity)(testRequest(testExtendedBusinessDetails(), legalEntity, false)) {
                    result =>
                      val document = Jsoup.parse(contentAsString(result))
                      val journey = JourneyConstants.getJourney(legalEntity)
                      val expectedSectionNumber = journey.indexOf(businessDetailsName) + 1
                      val totalSectionsForBusinessType = journey.size
                      val expectedSectionName = legalEntity match {
                        case "LLP_GRP" | "LTD_GRP" => Messages("awrs.index_page.group_business_details_text")
                        case "Partnership" | "LP" | "LLP" => Messages("awrs.index_page.partnership_details_text")
                        case _ => Messages("awrs.index_page.business_details_text")
                      }
                      document.getElementById("progress-text").text shouldBe Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  }
                }
            }
          }
      }

      allEntities.foreach {
        legalEntity =>
          s"$legalEntity" should {
            Seq(true, false).foreach {
              hasAwrs =>
                Seq(true, false).foreach {
                  isEditMode =>
                    s"correctly display/hide the business name section when isEditMode=$isEditMode and hasAwrs=$hasAwrs in" in {
                      getWithAuthorisedUser(isLinearjourney = !isEditMode, businessType = legalEntity, hasAwrs = hasAwrs)(testRequest(testExtendedBusinessDetails(), legalEntity, hasAwrs)) {
                        result =>
                          val document = Jsoup.parse(contentAsString(result))
                          val input = document.getElementById("companyName")
                          (hasAwrs, isEditMode, legalEntity) match {
                            case  (true, true, "LLP_GRP" | "LTD_GRP") => input.attr("value") shouldBe testBusinessCustomerDetails(legalEntity).businessName
                            case _ => input shouldBe null
                          }
                      }
                    }
                }
            }
          }
      }
    }

    def getWithAuthorisedUser(isNewApplication: Boolean = true, isLinearjourney: Boolean = true, businessType: String = "SOP", hasAwrs: Boolean = false)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      setUser(hasAwrs = hasAwrs)
      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
        fetchNewApplicationType = NewApplicationType(Some(false))
      )
      setupMockApiSave4LaterServiceWithOnly(fetchBusinessDetailsSupport = BusinessDetailsSupport(false))
      if (isNewApplication) {
        setupMockSave4LaterServiceWithOnly(fetchBusinessDetails = testBusinessDetails(newBusiness =
          Some(NewAWBusiness(newAWBusiness = "Yes", proposedStartDate = Some(TupleDate(day = "10", month = "10", year = "2016"))))))
      } else {
        setupMockSave4LaterServiceWithOnly(fetchBusinessDetails = None)
      }
      if (hasAwrs) {
        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
      } else {
        setAuthMocks(Future.successful(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), GGCredId("fakeCredID"))))
      }

      val result = testBusinessDetailsController.showBusinessDetails(isLinearjourney).apply(SessionBuilder.buildRequestWithSession(userId, businessType))
      test(result)
    }

    def getWithAuthorisedUserDataWithMissingStartDate(status: FormBundleStatus, haveDate: Boolean)(test: Future[Result] => Any): Unit = {
      val bd = testBusinessDetails(newBusiness = Some(NewAWBusiness(newAWBusiness = BooleanRadioEnum.YesString, proposedStartDate =
        haveDate match {
          case true => Some(TupleDate("01", "01", "2017"))
          case false => None
        })))
      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
        fetchBusinessDetails = Some(bd),
        fetchNewApplicationType = NewApplicationType(Some(false))
      )
      setupMockApiSave4LaterServiceWithOnly(fetchBusinessDetailsSupport = BusinessDetailsSupport(true)) // since this flag is already set we do not need to then also specify a bugged api5 data

      def buildRequestWithSession(userId: String, businessType: String) = {
        val sessionId = s"session-${UUID.randomUUID}"
        FakeRequest().withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.token -> "RANDOMTOKEN",
          SessionKeys.userId -> userId,
          "businessType" -> businessType,
          "businessName" -> "North East Wines",
          AwrsSessionKeys.sessionStatusType -> status.name
        )
      }
      setAuthMocks()

      val result = testBusinessDetailsController.showBusinessDetails(true).apply(buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))
      test(result)
    }
  }

}
