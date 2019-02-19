/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import _root_.models.BusinessDetailsEntityTypes._
import exceptions.{InvalidStateException, ResubmissionException}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.DataCacheKeys._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationServiceTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {
  val mockDataCacheService = mock[Save4LaterService]
  val mockEnrolService = mock[EnrolService]
  val mockEmailService = mock[EmailService]

  val subscribeSuccessResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val subscribeUpdateSuccessResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val updateGroupBusinessPartnerResponse = SuccessfulUpdateGroupBusinessPartnerResponse(processingDate = "2001-12-17T09:30:47Z")

  val baseAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("NE12 2DS"), None, Some("ES"))
  val baseSupplier = Supplier(alcoholSuppliers = "Yes",
    supplierName = Some("Smith and co"),
    vatRegistered = "Yes",
    vatNumber = testVrn,
    supplierAddress = Some(baseAddress),
    additionalSupplier = "No",
    ukSupplier = "No")

  def cachedData(legalEntity: BusinessType = testLegalEntity) =
    CacheMap(testUtr, Map("legalEntity" -> Json.toJson(legalEntity),
      "businessCustomerDetails" -> Json.toJson(testReviewDetails),
      businessDetailsName -> Json.toJson(testBusinessDetails()),
      businessRegistrationDetailsName -> Json.toJson(testBusinessRegistrationDetails(legalEntity = legalEntity.legalEntity.get)),
      placeOfBusinessName -> Json.toJson(testPlaceOfBusinessDefault()),
      businessContactsName -> Json.toJson(testBusinessContactsDefault()),
      "partnerDetails" -> Json.toJson(testPartnerDetails),
      "additionalBusinessPremises" -> Json.toJson(testAdditionalPremisesList),
      "businessDirectors" -> Json.toJson(testBusinessDirectors),
      tradingActivityName -> Json.toJson(testTradingActivity()),
      productsName -> Json.toJson(testProducts()),
      "suppliers" -> Json.toJson(testSupplierAddressList),
      "applicationDeclaration" -> Json.toJson(testApplicationDeclaration),
      groupMembersName -> Json.toJson(testGroupMemberDetails)
    ))

  def testSubscriptionTypeFrontEnd(legalEntity: Option[BusinessType] = Some(testLegalEntity),
                                   groupDeclaration: Option[GroupDeclaration] = Some(testGroupDeclaration),
                                   businessPartnerName: String = testBusinessPartnerName,
                                   // testBusinessCustomerDetailsOrig is the api 5 data which has the country field
                                   businessCustomerDetails: Option[BusinessCustomerDetails] = Some(testBusinessCustomerDetailsOrig),
                                   businessDetails: Option[BusinessDetails] = Some(testBusinessDetails()),
                                   businessRegistrationDetails: Option[BusinessRegistrationDetails] = Some(testBusinessRegistrationDetails(legalEntity = testLegalEntity.legalEntity.get)),
                                   businessContacts: Option[BusinessContacts] = Some(testBusinessContactsDefault()),
                                   placeOfBusiness: Option[PlaceOfBusiness] = Some(testPlaceOfBusinessDefault()),
                                   businessPartnerDetails: Option[Partners] = Some(testPartnerDetails),
                                   groupMemberDetails: Option[GroupMembers] = Some(testGroupMemberDetails),
                                   // testAdditionalPremisesListOrig is the api 5 data which has the main address at the top of the premise list
                                   additionalPremises: Option[AdditionalBusinessPremisesList] = Some(testAdditionalPremisesListOrig),
                                   businessDirectors: Option[BusinessDirectors] = Some(testBusinessDirectors),
                                   tradingActivity: Option[TradingActivity] = Some(testTradingActivity()),
                                   products: Option[Products] = Some(testProducts()),
                                   // testSupplierAddressListOrig is the api 5 data which does not have the country field
                                   suppliers: Option[Suppliers] = Some(testSupplierAddressListOrig),
                                   applicationDeclaration: Option[ApplicationDeclaration] = Some(testApplicationDeclaration),
                                   changeIndicators: Option[ChangeIndicators] = None) =
    SubscriptionTypeFrontEnd(
      legalEntity = legalEntity,
      businessPartnerName = businessPartnerName,
      groupDeclaration = groupDeclaration,
      businessCustomerDetails = businessCustomerDetails,
      businessDetails = businessDetails,
      businessRegistrationDetails = businessRegistrationDetails,
      placeOfBusiness = placeOfBusiness,
      businessContacts = businessContacts,
      partnership = businessPartnerDetails,
      groupMembers = groupMemberDetails,
      additionalPremises = additionalPremises,
      businessDirectors = businessDirectors,
      tradingActivity = tradingActivity,
      products = products,
      suppliers = suppliers,
      applicationDeclaration = applicationDeclaration,
      changeIndicators = changeIndicators
    )

  // the below feModel and cachedSubscription fields needs to be lazy because otherwise when ran from intellij if the
  // .as[AWRSFEModel] call fails it kills the entire test framework before any tests even starts to run
  lazy val feModel = api6LTDJson.as[AWRSFEModel]
  lazy val cachedSubscription = feModel.subscriptionTypeFrontEnd

  object TestApplicationService extends ApplicationService {
    override val save4LaterService = TestSave4LaterService
    override val keyStoreService = TestKeyStoreService
    override val enrolService = mockEnrolService
    override val awrsConnector = mockAWRSConnector
    override val emailService: EmailService = mockEmailService
  }

  "Application Service" should {
    "send data to right hand service and handle success" in {
      sendWithAuthorisedUser {
        result =>
          await(result) shouldBe subscribeSuccessResponse
      }
    }

    "send updated data to right hand service and handle success" in {
      sendUpdateSubscriptionTypeWithAuthorisedUser {
        result =>
          await(result) shouldBe subscribeUpdateSuccessResponse
      }
    }

    "send updated registration details to right hand service with a valid address and handle success" in {
      setupMockKeyStoreServiceForBusinessCustomerAddress()
      sendCallUpdateGroupBusinessPartnerWithAuthorisedUser {
        result =>
          await(result) shouldBe SuccessfulUpdateGroupBusinessPartnerResponse
      }
    }

    "have no address and handle success" in {
      setupMockKeyStoreServiceForBusinessCustomerAddress(noAddress = true)
      sendCallUpdateGroupBusinessPartnerWithAuthorisedUser {
        result =>
          await(result) shouldBe SuccessfulUpdateGroupBusinessPartnerResponse
      }
    }

    "have updated change Indicators in the subscription Type with the correct values" should {

      "with no changes" in {
        val thrown = the[ResubmissionException] thrownBy
          TestApplicationService.getModifiedSubscriptionType(
            Some(cachedData()),
            Some(testSubscriptionTypeFrontEnd())
          )
        thrown.getMessage shouldBe ResubmissionException.resubmissionMessage
      }

      "Group members Changed should update the correct flag" in {
        val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", " address Line2",
          Option(" address Line3"), Option(" address Line4"), Option(testPostcode)), " sap123", "safe123", false, Some("agent123"))

        val testGroupMemberOrig = GroupMember(CompanyNames(businessName = Some("Acme Ltd"), tradingName = Some("trading Name")), Some(Address(postcode = Some(testPostcode),
          addressLine1 = "Address Line 1", addressLine2 = "Address Line 2", addressLine3 = Some("Address Line 3 "), addressLine4 = Some(" Address Line 4 "),
          addressCountry = Some("United Kingdom"), addressCountryCode = Some("UK"))), groupJoiningDate = testGrpJoinDate, doYouHaveUTR = "Yes", utr = testUtr, isBusinessIncorporated = "Yes", companyRegDetails = Some(CompanyRegDetails(companyRegistrationNumber =
          testCrn, dateOfIncorporation = TupleDate("09", "06", "1985"))), doYouHaveVRN = "Yes", vrn = testVrn, addAnotherGrpMember = "No")

        val testGroupMemberDetailsOrig = GroupMembers(List(testGroupMemberOrig))

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(groupMemberDetails =
          Some(testGroupMemberDetailsOrig), businessCustomerDetails = Some(testBusinessCustomerDetailsOrig))))

        result.changeIndicators.get shouldBe getChangeIndicators(groupMembersChanged = true)
      }

      "Premises Changed should update the correct flag" in {
        val testAdditionalBusinessPremisesOrig = AdditionalBusinessPremises(additionalPremises = "No", Some(testAddress()), addAnother = "No")
        val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesOrig1, testAdditionalBusinessPremisesOrig))

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(
          additionalPremises = Some(testAdditionalPremisesListOrig))))

        result.changeIndicators.get shouldBe getChangeIndicators(premisesChanged = true)
      }

      "Suppliers Changed should update the correct flag" in {
        val testSupplier = Suppliers(List(Supplier(
          alcoholSuppliers = "Yes",
          supplierName = Some("SainsBurry"),
          vatRegistered = "Yes",
          vatNumber = testVrn,
          supplierAddress = Some(Address("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some(testPostcode))),
          additionalSupplier = "No",
          ukSupplier = "No")))

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSupplier))))

        result.changeIndicators.get shouldBe getChangeIndicators(suppliersChanged = true)
      }

      "Suppliers Changed should update the correct flag for Non UK Suppliers" in {
        val thrown = the[ResubmissionException] thrownBy TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSupplierAddressListOrig))))
        thrown.getMessage shouldBe ResubmissionException.resubmissionMessage
      }

      "Suppliers Changed should update the correct flag for UK Suppliers" in {
        val thrown = the[ResubmissionException] thrownBy TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSupplierAddressListOrig))))
        thrown.getMessage shouldBe ResubmissionException.resubmissionMessage
      }

      "Declaration Changed should update the correct flag" in {
        val testApplicationDeclarationOrig = ApplicationDeclaration(declarationName = Some("Paul Smith"), declarationRole = Some("Owner"), Option(true))

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(applicationDeclaration =
          Some(testApplicationDeclarationOrig))))

        result.changeIndicators.get shouldBe getChangeIndicators(declarationChanged = true)
      }

      "Coofficial Changed should update the correct flag" in {
        val testBusinessDirectorOrig = BusinessDirector(Some("Person"), firstName = Some("Paul"), lastName = Some("Smith"), doTheyHaveNationalInsurance = "Yes", nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Some("Director and Company Secretary"), otherDirectors = "No")

        val testBusinessDirectorsOrig = BusinessDirectors(List(testBusinessDirectorOrig))

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(businessDirectors = Some(testBusinessDirectorsOrig))))

        result.changeIndicators.get shouldBe getChangeIndicators(coOfficialsChanged = true)
      }

      "Trading Activity Changed should update the correct flag" in {
        val testTradingActivity = TradingActivity(
          wholesalerType = List("05", "99", "02"),
          otherWholesaler = "Walmart",
          typeOfAlcoholOrders = List("02", "04"),
          otherTypeOfAlcoholOrders = "Post",
          doesBusinessImportAlcohol = "Yes",
          thirdPartyStorage = "Yes",
          doYouExportAlcohol = Some("No"),
          exportLocation = None)

        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(tradingActivity = Some(testTradingActivity))))

        result.changeIndicators.get shouldBe getChangeIndicators(additionalBusinessInfoChanged = true)
      }

      "Prodcuts Changed should update the correct flag" in {
        val testProducts = Products(
          mainCustomers = List("02"),
          otherMainCustomers = Option("Off_License"),
          productType = List("01"),
          otherProductType = None)
        val result = TestApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(products = Some(testProducts))))

        result.changeIndicators.get shouldBe getChangeIndicators(additionalBusinessInfoChanged = true)
      }

      "Business Details changes should update the correct flag" should {

        lazy val changeIndicators = getChangeIndicators(businessDetailsChanged = true)

        "with Sole Trader Trading Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testSoleTraderBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()),
              tradingName = Some("Red Wines")
            )
          val testSoleTraderBusinessRegDetailsOrig = testSoleTraderBusinessRegistrationDetails
          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader VRN Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testSoleTraderBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()))
          val testSoleTraderBusinessRegDetailsOrig = testBusinessRegistrationDetails(doYouHaveNino = "Yes", nino = testNino, doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")
          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader NINO Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testSoleTraderBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()))
          val testSoleTraderBusinessRegDetailsOrig = testBusinessRegistrationDetails(doYouHaveNino = "Yes", nino = testNino, doYouHaveVRN = "No", doYouHaveUTR = "No")

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader UTR Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testSoleTraderBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()))
          val testSoleTraderBusinessRegDetailsOrig = testBusinessRegistrationDetails(doYouHaveNino = "Yes", nino = testNino, doYouHaveVRN = "No", doYouHaveUTR = "Yes", utr = testUtr)

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body Trading Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testCorporateBodyBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()),
              tradingName = Some("Red Wines")
            )
          val testCorporateBodyBusinessRegDetailsOrig = testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, isBusinessIncorporated = "No", doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testCorporateBodyBusinessDetailsOrig),
                businessRegistrationDetails = Some(testCorporateBodyBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body Company Registration Details Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testCorporateBodyBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness())
            )
          val testCorporateBodyBusinessRegDetailsOrig =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, isBusinessIncorporated = "No", companyRegDetails = Some(CompanyRegDetails(testCrn, TupleDate("09", "06", "1985"))), doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testCorporateBodyBusinessDetailsOrig),
                businessRegistrationDetails = Some(testCorporateBodyBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Details Trading Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testLlpBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()),
              tradingName = Some("Few boozes")
            )
          val testLlpBusinessRegDetailsOrig = testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, isBusinessIncorporated = "No", doYouHaveVRN = "Yes", vrn = Some("132456789"), doYouHaveUTR = "No")
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Details VRN Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testLlpBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness())
            )
          val testLlpBusinessRegDetailsOrig =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, isBusinessIncorporated = "No", doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Details UTR Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testLlpBusinessDetailsOrig =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness())
            )
          val testLlpBusinessRegDetailsOrig =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, isBusinessIncorporated = "No", doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "Yes", utr = testUtr)

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details Trading Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testPartnershipBusinessDetails =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness()),
              tradingName = Some("Red Wines")
            )
          val testPartnershipBusinessRegDetails =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details VRN Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testPartnershipBusinessDetails =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness())
            )
          val testPartnershipBusinessRegDetails =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "No")

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details UTR Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testPartnershipBusinessDetails =
            testBusinessDetails(
              newBusiness = Some(newAWBusiness())
            )
          val testPartnershipBusinessRegDetails =
            testBusinessRegistrationDetails(legalEntity = entityType.legalEntity, doYouHaveVRN = "Yes", vrn = testVrn, doYouHaveUTR = "Yes", utr = prefixedLowerCaseUTR.toString())

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }
      }

      "Business Address changes should update the correct flag" should {

        lazy val changeIndicators = getChangeIndicators(businessAddressChanged = true)

        "with Sole Trader Main Place Of Business Change" in {

          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress))
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader operation duration Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(operatingDuration = "3"))
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader Business Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testBusinessAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))
          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body Main Place Of Business Change Change" in {

          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body operation duration Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(operatingDuration = "7"))
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body Business Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Address Main Place Of Business Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Address Place of Business Address change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details Main Place Of Business Change" in {

          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details Place of Business Address change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

      }

      "Contact Details change should update the correct flag" should {

        lazy val changeIndicators = getChangeIndicators(contactDetailsChanged = true)

        "with Sole Trader contact Name Change" in {

          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Sole Trader contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Corporate Body contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))
          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Details Contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "with LLP Business Details Contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details Contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Dan")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get shouldBe changeIndicators
        }

        "with Partnership Business Details Contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            TestApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get shouldBe changeIndicators
        }

        "if trim suppliers is supplied with a list greater than 5 it will only return the first 5" in {
          val oldSuppliers = Suppliers(List(baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier))
          oldSuppliers.suppliers.size shouldBe 7
          val newSuppliers = TestApplicationService.trimSuppliers(Some(oldSuppliers))
          newSuppliers.get.suppliers.size shouldBe 5
        }

      }
    }

    "return the valid sections based on legal entity" should {
      "return valid Section object when legal entity is LTD" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData())
        val expectedSection = Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is SOP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("SOP")))
        val expectedSection = Sections(soleTraderBusinessDetails = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is Partnership" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("Partnership")))
        val expectedSection = Sections(partnershipBusinessDetails = true, partnership = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is LP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LP")))
        val expectedSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is LLP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LLP")))
        val expectedSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is LTD_GRP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LTD_GRP")))
        val expectedSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, businessDirectors = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return valid Section object when legal entity is LLP_GRP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LLP_GRP")))
        val expectedSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, partnership = true)
        val outputSection = await(TestApplicationService.getSections("cacheID"))
        outputSection shouldBe expectedSection
      }

      "return exception when legal entity is invalid" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("XYZ")))
        val thrown = the[InvalidStateException] thrownBy await(TestApplicationService.getSections("cacheID"))
        thrown.getMessage shouldBe "Invalid Legal entity"
      }
    }

    "remove the unnecessary attributes from subscription type " should {
      "return valid AWRSFEModel when entity type is LTD" in {
        val ltdSection = Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData()), Some(testBusinessCustomerDetails("LTD")), ltdSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "LTD"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe true

      }

      "return valid AWRSFEModel when entity type is SOP" in {
        val soleSection = Sections(soleTraderBusinessDetails = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("SOP"))),
          Some(testBusinessCustomerDetails("SOP")), soleSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "SOP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe false

      }

      "return valid AWRSFEModel when entity type is Partnership" in {
        val partnershipSection = Sections(partnershipBusinessDetails = true, partnership = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("Partnership"))),
          Some(testBusinessCustomerDetails("Partnership")), partnershipSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "Partnership"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe false

      }

      "return valid AWRSFEModel when entity type is LP" in {
        val lpSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LP"))),
          Some(testBusinessCustomerDetails("LP")), lpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "LP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe false

      }

      "return valid AWRSFEModel when entity type is LLP" in {
        val llpSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LLP"))),
          Some(testBusinessCustomerDetails("LLP")), llpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "LLP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe false

      }

      "return valid AWRSFEModel when entity type is LTD_GRP" in {
        val ltdGrpSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, businessDirectors = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LTD_GRP"))),
          Some(testBusinessCustomerDetails("LTD_GRP")), ltdGrpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "LTD_GRP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe true

      }

      "return valid AWRSFEModel when entity type is LLP_GRP" in {
        val llpGrpSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, partnership = true)
        val outputSubscriptionType = TestApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LLP_GRP"))),
          Some(testBusinessCustomerDetails("LLP_GRP")), llpGrpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get shouldBe "LLP_GRP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined shouldBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined shouldBe false
      }
    }

    "isApplicationDifferent " should {
      "return false if no section has changed" in {
        val result = TestApplicationService.isApplicationDifferent(matchingSoleTraderCacheMap, soleTraderSubscriptionTypeFrontEnd)
        await(result) shouldBe false
      }

      "return true if any section has changed" in {
        val result = TestApplicationService.isApplicationDifferent(matchingSoleTraderCacheMap, differentSoleTraderSubscriptionTypeFrontEnd)
        await(result) shouldBe true
      }
    }

    "hasAPI5ApplicationChanged " should {
      "return false if no section has changed" in {
        setupMockSave4LaterService(fetchAll = matchingSoleTraderCacheMap)
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = soleTraderSubscriptionTypeFrontEnd)
        val result = TestApplicationService.hasAPI5ApplicationChanged(testUtr)
        await(result) shouldBe false
      }

      "return true if any section has changed" in {
        setupMockSave4LaterService(fetchAll = matchingSoleTraderCacheMap)
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = differentSoleTraderSubscriptionTypeFrontEnd)
        val result = TestApplicationService.hasAPI5ApplicationChanged(testUtr)
        await(result) shouldBe true
      }
    }

    "getApi5ChangeIndicators " should {
      "return all false indicators if no section has changed" in {
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = soleTraderSubscriptionTypeFrontEnd)
        val result = TestApplicationService.getApi5ChangeIndicators(matchingSoleTraderCacheMap)
        await(result) shouldBe SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false)
      }

      "return at least one true indicator if any section has changed" in {
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = differentSoleTraderSubscriptionTypeFrontEnd)
        val result = TestApplicationService.getApi5ChangeIndicators(matchingSoleTraderCacheMap)
        await(result) shouldBe SectionChangeIndicators(true, false, false, false, false, false, false, false, false, false, false, false)
      }
    }
    "addGroupRepToGroupMembers " should {
      "add the group rep as the first group member to the GroupMembers list" in {
        val result = testAddGroupRepToGroupMembers
        result._2.get.members.size shouldBe  result._1.getEntry[GroupMembers](groupMembersName).get.members.size + 1
      }

      "have the first group member as the group rep" in {
        val result = testAddGroupRepToGroupMembers
        result._2.get.members(0).companyNames.tradingName shouldBe result._1.getEntry[BusinessDetails](businessDetailsName).get.tradingName
        result._2.get.members(0).companyNames.businessName.get shouldBe result._1.getEntry[BusinessCustomerDetails]("businessCustomerDetails").get.businessName
      }
    }

    "replaceGroupRepInGroupMembers " should {
      "not change the size of the GroupMembers collection" in {
        val result = testReplaceGroupRepInGroupMembers
        result._2.get.members.size shouldBe  result._1.getEntry[GroupMembers](groupMembersName).get.members.size
      }
      "contain a different business name in the group rep group member" in {
        val result = testReplaceGroupRepInGroupMembers
        result._2.get.members(0).companyNames.businessName.get should not be result._1.getEntry[BusinessCustomerDetails]("businessCustomerDetails").get.businessName
      }
    }

    "isGrpRepChanged" should {
      allEntities.foreach {
        legalEntity =>
          Seq(true, false).foreach {
            updatedBusinessName =>
              s"return the correct value for legalEntity: $legalEntity and updatedBusinessName: $updatedBusinessName" in {
                val businessType = BusinessType(Some(legalEntity), None, Some(true))
                (updatedBusinessName, legalEntity) match {
                  case (true, ("LTD_GRP" | "LLP_GRP")) => TestApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "Changed")) shouldBe true
                  case (true, _) => TestApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "Changed")) shouldBe false
                  case _ => TestApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME")) shouldBe false
                }
              }
          }
      }
    }
  }

  def testAddGroupRepToGroupMembers: (CacheMap,Option[GroupMembers]) = {
    val cached = cachedData()
    (cached, TestApplicationService.addGroupRepToGroupMembers(Some(cached)))
  }

  def testReplaceGroupRepInGroupMembers: (CacheMap, Option[GroupMembers]) = {
    val cached = cachedData()
    val legalEntity = testLegalEntity.legalEntity.get
    val changed = CacheMap(testUtr, Map("legalEntity" -> Json.toJson(legalEntity),
      businessCustomerDetailsName -> Json.toJson(testBusinessNameChanged),
      businessDetailsName -> Json.toJson(testBusinessDetails()),
      businessRegistrationDetailsName -> Json.toJson(testBusinessRegistrationDetails(legalEntity)),
      placeOfBusinessName -> Json.toJson(testPlaceOfBusinessDefault()),
      groupMembersName -> Json.toJson(testGroupMemberDetails)))

    (cached, TestApplicationService.replaceGroupRepInGroupMembers(Some(changed)))
  }

  def sendWithAuthorisedUser(test: Future[SuccessfulSubscriptionResponse] => Any): Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockAWRSConnectorWithOnly(submitAWRSData = subscribeSuccessResponse)
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    when(mockEmailService.sendConfirmationEmail(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    val result = TestApplicationService.sendApplication()
    test(result)
  }

  def sendUpdateSubscriptionTypeWithAuthorisedUser(test: Future[SuccessfulSubscriptionResponse] => Any): Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = cachedSubscription)
    setupMockAWRSConnectorWithOnly(updateAWRSData = subscribeUpdateSuccessResponse)
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    TestApplicationService.updateApplication()
  }

  def sendCallUpdateGroupBusinessPartnerWithAuthorisedUser(test: Future[SuccessfulUpdateGroupBusinessPartnerResponse] => Any) : Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = cachedSubscription)
    setupMockAWRSConnectorWithOnly(updateGroupBusinessPartner = updateGroupBusinessPartnerResponse)
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    TestApplicationService.callUpdateGroupBusinessPartner(cachedData(), Some(cachedSubscription), testSubscriptionStatusTypeApproved)
  }
}
