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

package services

import _root_.models.BusinessDetailsEntityTypes._
import exceptions.{InvalidStateException, ResubmissionException}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.DataCacheKeys._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationServiceTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {
  val mockDataCacheService: Save4LaterService = mock[Save4LaterService]
  val mockEmailService: EmailService = mock[EmailService]

  val selfHealSuccessResponse: SelfHealSubscriptionResponse = SelfHealSubscriptionResponse(regimeRefNumber = "12345")
  val subscribeSuccessResponse: SuccessfulSubscriptionResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val subscribeUpdateSuccessResponse: SuccessfulUpdateSubscriptionResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val updateGroupBusinessPartnerResponse: SuccessfulUpdateGroupBusinessPartnerResponse = SuccessfulUpdateGroupBusinessPartnerResponse(processingDate = "2001-12-17T09:30:47Z")

  val baseAddress: Address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("NE12 2DS"), None, Some("ES"))
  val baseSupplier: Supplier = Supplier(alcoholSuppliers = "Yes",
    supplierName = Some("Smith and co"),
    vatRegistered = "Yes",
    vatNumber = testVrn,
    supplierAddress = Some(baseAddress),
    additionalSupplier = "No",
    ukSupplier = "No")

  override def beforeEach(): Unit = {
    reset(mockAccountUtils)
    reset(mockKeyStoreConnector)
    when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
      .thenReturn(true)

    super.beforeEach()
  }


  def cachedData(legalEntity: BusinessType = testLegalEntity): CacheMap =
    CacheMap(testUtr, Map("legalEntity" -> Json.toJson(legalEntity),
      "businessCustomerDetails" -> Json.toJson(testReviewDetails),
      businessNameDetailsName -> Json.toJson(testBusinessNameDetails()),
      tradingStartDetailsName -> Json.toJson(newAWBusiness(proposedStartDate = None)),
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

  def cachedDataNoSupplier(): CacheMap =
    CacheMap(testUtr, Map("legalEntity" -> Json.toJson(testLegalEntity),
      "businessCustomerDetails" -> Json.toJson(testReviewDetails),
      businessNameDetailsName -> Json.toJson(testBusinessNameDetails()),
      tradingStartDetailsName -> Json.toJson(newAWBusiness(proposedStartDate = None)),
      businessRegistrationDetailsName -> Json.toJson(testBusinessRegistrationDetails(legalEntity = testLegalEntity.legalEntity.get)),
      placeOfBusinessName -> Json.toJson(testPlaceOfBusinessDefault()),
      businessContactsName -> Json.toJson(testBusinessContactsDefault()),
      "partnerDetails" -> Json.toJson(testPartnerDetails),
      "additionalBusinessPremises" -> Json.toJson(testAdditionalPremisesList),
      "businessDirectors" -> Json.toJson(testBusinessDirectors),
      tradingActivityName -> Json.toJson(testTradingActivity()),
      productsName -> Json.toJson(testProducts()),
      "applicationDeclaration" -> Json.toJson(testApplicationDeclaration),
      groupMembersName -> Json.toJson(testGroupMemberDetails)
    ))

  def cachedDataWithUtr(legalEntity: BusinessType = testLegalEntity, utr:Option[String] = None): CacheMap =
    CacheMap(testUtr, Map("legalEntity" -> Json.toJson(legalEntity),
      "businessCustomerDetails" -> Json.toJson(testReviewDetails),
      businessNameDetailsName -> Json.toJson(testBusinessNameDetails()),
      tradingStartDetailsName -> Json.toJson(newAWBusiness(proposedStartDate = None)),
      businessRegistrationDetailsName -> Json.toJson(testBusinessRegistrationDetails(legalEntity = legalEntity.legalEntity.get, utr = utr)),
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
                                   changeIndicators: Option[ChangeIndicators] = None): SubscriptionTypeFrontEnd =
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
  lazy val feModel: AWRSFEModel = api6LTDJson.as[AWRSFEModel]
  lazy val cachedSubscription: SubscriptionTypeFrontEnd = feModel.subscriptionTypeFrontEnd

  val testApplicationService: ApplicationService = new ApplicationService(mockAWRSConnector, mockEmailService, testSave4LaterService, testKeyStoreService, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector)

  "Application Service" must {
    "getRegistrationReferenceNumber must return left when given self heal case " in {

      testApplicationService.getRegistrationReferenceNumber(Left(selfHealSuccessResponse)) mustBe "12345"
    }

    "getRegistrationReferenceNumber must return right when given subscription case " in {

      testApplicationService.getRegistrationReferenceNumber(Right(subscribeSuccessResponse)) mustBe "ABCDEabcde12345"
    }

    "send application and handle a 200 success" in {
      sendWithAuthorisedUser {
        result =>
          await(result) mustBe Right(subscribeSuccessResponse)
      }
    }

      "send application to self heal and handle 202 response" in {
        sendWithAuthorisedUserSelfHeal {
          result =>
            await(result) mustBe Left(selfHealSuccessResponse)
        }
      }

      "send updated subscription and handle a 200 success" in {
        sendUpdateSubscriptionTypeWithAuthorisedUser {
          result =>
            await(result) mustBe subscribeUpdateSuccessResponse
        }
      }

      "send updated subscription and handle a 200 success for a new business" in {
        sendUpdateSubscriptionTypeWithAuthorisedUser (
          result =>
            await(result) mustBe subscribeUpdateSuccessResponse
        , newAW = true)
      }

      "send updated registration details to service with a valid address and handle success" in {
        setupMockKeyStoreServiceForBusinessCustomerAddress()
        sendCallUpdateGroupBusinessPartnerWithAuthorisedUser {
          result =>
            await(result) mustBe SuccessfulUpdateGroupBusinessPartnerResponse
        }
      }

      "have no address and handle success" in {
        setupMockKeyStoreServiceForBusinessCustomerAddress(noAddress = true)
        sendCallUpdateGroupBusinessPartnerWithAuthorisedUser {
          result =>
            await(result) mustBe SuccessfulUpdateGroupBusinessPartnerResponse
        }
      }

    "have updated change Indicators in the subscription Type with the correct values" must {

      "with no changes" in {
        val thrown = the[ResubmissionException] thrownBy
          testApplicationService.getModifiedSubscriptionType(
            Some(cachedData()),
            Some(testSubscriptionTypeFrontEnd())
          )
        thrown.getMessage mustBe ResubmissionException.resubmissionMessage
      }

      "Group members Changed must update the correct flag" in {
        val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", " address Line2",
          Option(" address Line3"), Option(" address Line4"), Option(testPostcode)), " sap123", "safe123", false, Some("agent123"))

        val testGroupMemberOrig = GroupMember(CompanyNames(businessName = Some("Acme Ltd"), tradingName = Some("trading Name")), Some(Address(postcode = Some(testPostcode),
          addressLine1 = "Address Line 1", addressLine2 = "Address Line 2", addressLine3 = Some("Address Line 3 "), addressLine4 = Some(" Address Line 4 "),
          addressCountry = Some("United Kingdom"), addressCountryCode = Some("UK"))), groupJoiningDate = testGrpJoinDate, doYouHaveUTR = "Yes", utr = testUtr, isBusinessIncorporated = "Yes", companyRegDetails = Some(CompanyRegDetails(companyRegistrationNumber =
          testCrn, dateOfIncorporation = TupleDate("09", "06", "1985"))), doYouHaveVRN = "Yes", vrn = testVrn, addAnotherGrpMember = "No")

        val testGroupMemberDetailsOrig = GroupMembers(List(testGroupMemberOrig))

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(groupMemberDetails =
          Some(testGroupMemberDetailsOrig), businessCustomerDetails = Some(testBusinessCustomerDetailsOrig))))

        result.changeIndicators.get mustBe getChangeIndicators(groupMembersChanged = true)
      }

      "Premises Changed must update the correct flag" in {
        val testAdditionalBusinessPremisesOrig = AdditionalBusinessPremises(additionalPremises = "No", Some(testAddress()), addAnother = "No")
        val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesOrig1, testAdditionalBusinessPremisesOrig))

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(
          additionalPremises = Some(testAdditionalPremisesListOrig))))

        result.changeIndicators.get mustBe getChangeIndicators(premisesChanged = true)
      }

      "Suppliers Changed must update the correct flag" in {
        val testSupplier = Suppliers(List(Supplier(
          alcoholSuppliers = "Yes",
          supplierName = Some("SainsBurry"),
          vatRegistered = "Yes",
          vatNumber = testVrn,
          supplierAddress = Some(Address("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some(testPostcode))),
          additionalSupplier = "No",
          ukSupplier = "No")))

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSupplier))))

        result.changeIndicators.get mustBe getChangeIndicators(suppliersChanged = true)
      }

      "Suppliers Changed must update the correct flag for Non UK Suppliers" in {
        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSuppliersInternational))))
        result.changeIndicators.get mustBe getChangeIndicators(suppliersChanged = true)
      }

      "Suppliers Changed must update the correct flag for UK Suppliers" in {
        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(suppliers = Some(testSuppliers))))
        result.changeIndicators.get mustBe getChangeIndicators(suppliersChanged = true)
      }

      "Suppliers Changed must update the correct flag when no change to suppliers" in {
        val result =  testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(businessPartnerDetails = Some(testPartnerDetailsSingle))))
        result.changeIndicators.get mustBe getChangeIndicators(suppliersChanged = false, partnersChanged = true)
      }

      "Suppliers Changed must update the correct flag when no suppliers found in user answers" in {
        val result =  testApplicationService.getModifiedSubscriptionType(Some(cachedDataNoSupplier()), Some(testSubscriptionTypeFrontEnd()))
        result.changeIndicators.get mustBe getChangeIndicators(suppliersChanged = true)
      }

      "Declaration Changed must update the correct flag" in {
        val testApplicationDeclarationOrig = ApplicationDeclaration(declarationName = Some("Paul Smith"), declarationRole = Some("Owner"), Option(true))

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(applicationDeclaration =
          Some(testApplicationDeclarationOrig))))

        result.changeIndicators.get mustBe getChangeIndicators(declarationChanged = true)
      }

      "Coofficial Changed must update the correct flag" in {
        val testBusinessDirectorOrig = BusinessDirector(Some("Person"), firstName = Some("Paul"), lastName = Some("Smith"), doTheyHaveNationalInsurance = "Yes", nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Some("Director and Company Secretary"), otherDirectors = "No")

        val testBusinessDirectorsOrig = BusinessDirectors(List(testBusinessDirectorOrig))

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(businessDirectors = Some(testBusinessDirectorsOrig))))

        result.changeIndicators.get mustBe getChangeIndicators(coOfficialsChanged = true)
      }

      "Trading Activity Changed must update the correct flag" in {
        val testTradingActivity = TradingActivity(
          wholesalerType = List("05", "99", "02"),
          otherWholesaler = "Walmart",
          typeOfAlcoholOrders = List("02", "04"),
          otherTypeOfAlcoholOrders = "Post",
          doesBusinessImportAlcohol = "Yes",
          thirdPartyStorage = "Yes",
          doYouExportAlcohol = Some("No"),
          exportLocation = None)

        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(tradingActivity = Some(testTradingActivity))))

        result.changeIndicators.get mustBe getChangeIndicators(additionalBusinessInfoChanged = true)
      }

      "Prodcuts Changed must update the correct flag" in {
        val testProducts = Products(
          mainCustomers = List("02"),
          otherMainCustomers = Option("Off_License"),
          productType = List("01"),
          otherProductType = None)
        val result = testApplicationService.getModifiedSubscriptionType(Some(cachedData()), Some(testSubscriptionTypeFrontEnd(products = Some(testProducts))))

        result.changeIndicators.get mustBe getChangeIndicators(additionalBusinessInfoChanged = true)
      }

      "Business Details changes must update the correct flag" must {

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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testSoleTraderBusinessDetailsOrig),
                businessRegistrationDetails = Some(testSoleTraderBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault()),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testCorporateBodyBusinessDetailsOrig),
                businessRegistrationDetails = Some(testCorporateBodyBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testCorporateBodyBusinessDetailsOrig),
                businessRegistrationDetails = Some(testCorporateBodyBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))
          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testLlpBusinessDetailsOrig),
                businessRegistrationDetails = Some(testLlpBusinessRegDetailsOrig),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
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
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessDetails = Some(testPartnershipBusinessDetails),
                businessRegistrationDetails = Some(testPartnershipBusinessRegDetails),
                businessContacts = Some(testBusinessContactsDefault())
              )))

          result.changeIndicators.get mustBe changeIndicators
        }
      }

      "Business Address changes must update the correct flag" must {

        lazy val changeIndicators = getChangeIndicators(businessAddressChanged = true)

        "with Sole Trader Main Place Of Business Change" in {

          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress))
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Sole Trader operation duration Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(operatingDuration = "3"))
              )))
          result.changeIndicators.get mustBe changeIndicators
        }

        "with Sole Trader Business Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testBusinessAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))
          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))
          result.changeIndicators.get mustBe changeIndicators
        }

        "with Corporate Body Main Place Of Business Change Change" in {

          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Corporate Body operation duration Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(operatingDuration = "7"))
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Corporate Body Business Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with LLP Business Address Main Place Of Business Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with LLP Business Address Place of Business Address change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Partnership Business Details Main Place Of Business Change" in {

          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val newAddress = Address("my address", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE28 8ER"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = newAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Partnership Business Details Place of Business Address change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessAddress = Address("address Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                placeOfBusiness = Some(testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testBusinessAddress))
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

      }

      "Contact Details change must update the correct flag" must {

        lazy val changeIndicators = getChangeIndicators(contactDetailsChanged = true)

        "with Sole Trader contact Name Change" in {

          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Sole Trader contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(SoleTrader)
          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val testAddressMainPlace = Address(postcode = Some("postcode"), addressLine1 = "line1Changed", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("country"))
          val testAdditionalBusinessPremisesMainPlace = AdditionalBusinessPremises(additionalPremises = "Yes", Some(testAddressMainPlace), addAnother = "No")
          val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesMainPlace, testAdditionalBusinessPremises))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                additionalPremises = Some(testAdditionalPremisesListOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Corporate Body contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
        }

        "with Corporate Body contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(CorporateBody)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))
          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with LLP Business Details Contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Mark")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
        }

        "with LLP Business Details Contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(Llp)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Partnership Business Details Contact Name Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactFirstName = "Dan")),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))

          result.changeIndicators.get mustBe changeIndicators
        }

        "with Partnership Business Details Contact Address Change" in {
          val entityType = testBusinessDetailsEntityTypes(Partnership)
          val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("address Line1", "address Line2", Option("address Line3"), Option("address Line4"), Option("NE28 8ER")), "sap123", "safe123", false, Some("agent123"))

          val testContactAddress = Address("Line1", "address Line2", Some("address Line3"), Some("address Line4"), Some("NE2 1AP"))

          val result =
            testApplicationService.getModifiedSubscriptionType(
              Some(cachedData(legalEntity = entityType)),
              Some(testSubscriptionTypeFrontEnd(
                legalEntity = Some(entityType),
                businessRegistrationDetails = Some(testBusinessRegistrationDetails(legalEntity = entityType.legalEntity.get)),
                businessContacts = Some(testBusinessContactsDefault(contactAddress = testContactAddress)),
                businessCustomerDetails = Some(testBusinessCustomerDetailsOrig)
              )))
          result.changeIndicators.get mustBe changeIndicators
        }

        "if trim suppliers is supplied with a list greater than 5 it will only return the first 5" in {
          val oldSuppliers = Suppliers(List(baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier, baseSupplier))
          oldSuppliers.suppliers.size mustBe 7
          val newSuppliers = testApplicationService.trimSuppliers(Some(oldSuppliers))
          newSuppliers.get.suppliers.size mustBe 5
        }

      }
    }

    "return the valid sections based on legal entity" must {
      "return valid Section object when legal entity is LTD" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData())
        val expectedSection = Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is SOP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("SOP")))
        val expectedSection = Sections(soleTraderBusinessDetails = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is Partnership" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("Partnership")))
        val expectedSection = Sections(partnershipBusinessDetails = true, partnership = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is LP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LP")))
        val expectedSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is LLP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LLP")))
        val expectedSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is LTD_GRP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LTD_GRP")))
        val expectedSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, businessDirectors = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return valid Section object when legal entity is LLP_GRP" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("LLP_GRP")))
        val expectedSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, partnership = true)
        val outputSection = await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        outputSection mustBe expectedSection
      }

      "return exception when legal entity is invalid" in {
        setupMockSave4LaterServiceWithOnly(fetchAll = cachedData(dynamicLegalEntity("XYZ")))
        val thrown = the[InvalidStateException] thrownBy await(testApplicationService.getSections("cacheID", TestUtil.defaultAuthRetrieval))
        thrown.getMessage mustBe "Invalid Legal entity"
      }
    }

    "remove the unnecessary attributes from subscription type " must {
      "return valid AWRSFEModel when business registration details utr has spaces" in {
        val ltdSection = Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedDataWithUtr(utr = Some("11 111 111 11"))), Some(testBusinessCustomerDetails("LTD")), ltdSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.utr.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.utr.get mustBe "1111111111"


      }

      "return valid AWRSFEModel when entity type is LTD" in {
        val ltdSection = Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData()), Some(testBusinessCustomerDetails("LTD")), ltdSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "LTD"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe true

      }

      "return valid AWRSFEModel when entity type is SOP" in {
        val soleSection = Sections(soleTraderBusinessDetails = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("SOP"))),
          Some(testBusinessCustomerDetails("SOP")), soleSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "SOP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe false

      }

      "return valid AWRSFEModel when entity type is Partnership" in {
        val partnershipSection = Sections(partnershipBusinessDetails = true, partnership = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("Partnership"))),
          Some(testBusinessCustomerDetails("Partnership")), partnershipSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "Partnership"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe false

      }

      "return valid AWRSFEModel when entity type is LP" in {
        val lpSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LP"))),
          Some(testBusinessCustomerDetails("LP")), lpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "LP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe false

      }

      "return valid AWRSFEModel when entity type is LLP" in {
        val llpSection = Sections(llpBusinessDetails = true, partnership = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LLP"))),
          Some(testBusinessCustomerDetails("LLP")), llpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "LLP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe false

      }

      "return valid AWRSFEModel when entity type is LTD_GRP" in {
        val ltdGrpSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, businessDirectors = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LTD_GRP"))),
          Some(testBusinessCustomerDetails("LTD_GRP")), ltdGrpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "LTD_GRP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe false
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe true

      }

      "return valid AWRSFEModel when entity type is LLP_GRP" in {
        val llpGrpSection = Sections(groupRepBusinessDetails = true, groupMemberDetails = true, partnership = true)
        val outputSubscriptionType = testApplicationService.assembleAWRSFEModel(Some(cachedData(dynamicLegalEntity("LLP_GRP"))),
          Some(testBusinessCustomerDetails("LLP_GRP")), llpGrpSection)

        outputSubscriptionType.subscriptionTypeFrontEnd.businessDetails.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessRegistrationDetails.get.legalEntity.get mustBe "LLP_GRP"
        outputSubscriptionType.subscriptionTypeFrontEnd.businessContacts.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.partnership.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.groupMembers.isDefined mustBe true
        outputSubscriptionType.subscriptionTypeFrontEnd.businessDirectors.isDefined mustBe false
      }
    }

    "isApplicationDifferent " must {
      "return false if no section has changed" in {
        val result = testApplicationService.isApplicationDifferent(matchingSoleTraderCacheMap, soleTraderSubscriptionTypeFrontEnd)
        await(result) mustBe false
      }

      "return true if any section has changed" in {
        val result = testApplicationService.isApplicationDifferent(matchingSoleTraderCacheMap, differentSoleTraderSubscriptionTypeFrontEnd)
        await(result) mustBe true
      }
    }

    "hasAPI5ApplicationChanged " must {
      "return false if no section has changed" in {
        setupMockSave4LaterService(fetchAll = matchingSoleTraderCacheMap)
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = soleTraderSubscriptionTypeFrontEnd)
        val result = testApplicationService.hasAPI5ApplicationChanged(testUtr, TestUtil.defaultAuthRetrieval)
        await(result) mustBe false
      }

      "return true if any section has changed" in {
        setupMockSave4LaterService(fetchAll = matchingSoleTraderCacheMap)
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = differentSoleTraderSubscriptionTypeFrontEnd)
        val result = testApplicationService.hasAPI5ApplicationChanged(testUtr, TestUtil.defaultAuthRetrieval)
        await(result) mustBe true
      }
    }

    "getApi5ChangeIndicators " must {
      "return all false indicators if no section has changed" in {
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = soleTraderSubscriptionTypeFrontEnd)
        val result = testApplicationService.getApi5ChangeIndicators(matchingSoleTraderCacheMap, TestUtil.defaultAuthRetrieval)
        await(result) mustBe SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false)
      }

      "return all false indicators there is no AWRS enrolment" in {
        when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
          .thenReturn(false)
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = Future.successful(None))
        val result = testApplicationService.getApi5ChangeIndicators(matchingSoleTraderCacheMap, TestUtil.defaultAuthRetrieval)
        await(result) mustBe SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false)
      }

      "return at least one true indicator if any section has changed" in {
        setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = differentSoleTraderSubscriptionTypeFrontEnd)
        val result = testApplicationService.getApi5ChangeIndicators(matchingSoleTraderCacheMap, TestUtil.defaultAuthRetrieval)
        await(result) mustBe SectionChangeIndicators(true, false, false, false, false, false, false, false, false, false, false)
      }
    }
    "addGroupRepToGroupMembers " must {
      "add the group rep as the first group member to the GroupMembers list" in {
        val result = testAddGroupRepToGroupMembers
        result._2.get.members.size mustBe  result._1.getEntry[GroupMembers](groupMembersName).get.members.size + 1
      }

      "have the first group member as the group rep" in {
        val result = testAddGroupRepToGroupMembers
        result._2.get.members(0).companyNames.tradingName mustBe result._1.getEntry[BusinessNameDetails](businessNameDetailsName).get.tradingName
        result._2.get.members(0).companyNames.businessName.get mustBe result._1.getEntry[BusinessCustomerDetails]("businessCustomerDetails").get.businessName
      }
    }

    "replaceGroupRepInGroupMembers " must {
      "not change the size of the GroupMembers collection" in {
        val result = testReplaceGroupRepInGroupMembers
        result._2.get.members.size mustBe  result._1.getEntry[GroupMembers](groupMembersName).get.members.size
      }
      "contain a different business name in the group rep group member" in {
        val result = testReplaceGroupRepInGroupMembers
        result._2.get.members(0).companyNames.businessName.get must not be result._1.getEntry[BusinessCustomerDetails]("businessCustomerDetails").get.businessName
      }
    }

    "isGrpRepChanged" must {
      allEntities.foreach {
        legalEntity =>
          Seq(true, false).foreach {
            updatedBusinessName =>
              s"return the correct value for legalEntity: $legalEntity and updatedBusinessName: $updatedBusinessName" in {
                val businessType = BusinessType(Some(legalEntity), None, Some(true))
                (updatedBusinessName, legalEntity) match {
                  case (true, ("LTD_GRP" | "LLP_GRP")) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "Changed")) mustBe true
                  case (false, ("LTD_GRP" | "LLP_GRP")) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME")) mustBe false
                  case (true, _) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "Changed")) mustBe false
                  case _ => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME")) mustBe false
                }
              }
          }
          Seq(true, false).foreach {
            updatedAddress =>
              s"return the correct value for legalEntity: $legalEntity and updatedAddress: $updatedAddress" in {
                val businessType = BusinessType(Some(legalEntity), None, Some(true))
                (updatedAddress, legalEntity) match {
                  case (true, ("LTD_GRP" | "LLP_GRP")) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME", placeOfBusiness = Some(testPlaceOfBusinessDefault(mainAddress = testAddress)))) mustBe true
                  case (false, ("LTD_GRP" | "LLP_GRP")) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME")) mustBe false
                  case (true, _) => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME", placeOfBusiness = testPlaceOfBusinessDefault(mainAddress = testAddress))) mustBe false
                  case _ => testApplicationService.isGrpRepChanged(cachedData(businessType), testSubscriptionTypeFrontEnd(businessPartnerName = "ACME")) mustBe false
                }
              }
          }

      }
    }

    "getChangeIndicators" must {
      "manage legalEntity changes" when {
        val typesOfLegalEntityChanges = Map(
          ("SOP", "LLP") -> SectionChangeIndicators(partnersChanged = true),
          ("SOP", "LTD") -> SectionChangeIndicators(coOfficialsChanged = true),
          ("SOP", "LTD_GRP") -> SectionChangeIndicators(groupMembersChanged = true, coOfficialsChanged = true),
          ("SOP", "LLP_GRP") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true),
          ("LTD", "LLP") -> SectionChangeIndicators(partnersChanged = true, coOfficialsChanged = true),
          ("LTD", "SOP") -> SectionChangeIndicators(coOfficialsChanged = true),
          ("LTD", "LTD_GRP") -> SectionChangeIndicators(groupMembersChanged = true),
          ("LTD", "LLP_GRP") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true),
          ("LLP", "LTD") -> SectionChangeIndicators(partnersChanged = true, coOfficialsChanged = true),
          ("LLP", "SOP") -> SectionChangeIndicators(partnersChanged = true),
          ("LLP", "LTD_GRP") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true),
          ("LLP", "LLP_GRP") -> SectionChangeIndicators(groupMembersChanged = true, coOfficialsChanged = true),
          ("LTD_GRP", "LTD") -> SectionChangeIndicators(groupMembersChanged = true),
          ("LTD_GRP", "SOP") -> SectionChangeIndicators(groupMembersChanged = true, coOfficialsChanged = true),
          ("LTD_GRP", "LLP") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true),
          ("LTD_GRP", "LLP_GRP") -> SectionChangeIndicators(partnersChanged = true, coOfficialsChanged = true),
          ("LLP_GRP", "LTD") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true),
          ("LLP_GRP", "SOP") -> SectionChangeIndicators(groupMembersChanged = true, partnersChanged = true),
          ("LLP_GRP", "LLP") -> SectionChangeIndicators(groupMembersChanged = true, coOfficialsChanged = true),
          ("LLP_GRP", "LTD_GRP") -> SectionChangeIndicators(partnersChanged = true, coOfficialsChanged = true)
        )

        typesOfLegalEntityChanges foreach {
          case ((data, otherData), expectedChangeIndicators) =>
            s"${data} becomes ${otherData}" in {
              val businessTypeData = BusinessType(Some(data), None, Some(true))
              val businessTypeOther = BusinessType(Some(otherData), None, Some(true))

              val changeInds = testApplicationService.getChangeIndicators(
                cachedData(businessTypeOther),
                testSubscriptionTypeFrontEnd(legalEntity = Some(businessTypeData), businessRegistrationDetails = testBusinessRegistrationDetails(legalEntity = data))
              )

              changeInds.get mustBe expectedChangeIndicators.copy(
                businessDetailsChanged = otherData.contains("GRP"),
                businessRegistrationDetailsChanged = true
              )
            }
        }
      }
    }

    "isNewBusiness" must {
      "return true if the business is classed as a 'New Business' (trading alcohol after 31st March 2016 and registering to trade in alcohol)" in {
        val cached = Some(CacheMap(testUtr, Map(
          tradingStartDetailsName -> Json.toJson(NewAWBusiness("No", Some(TupleDate("01", "01", "2019"))))
        )))

        await(testApplicationService.isNewBusiness(cached)) mustBe true
      }

      "return false if the business is classed as a 'NewAWBusiness' (trading alcohol before 1st April 2016 and newly registering for AWRS)" in {
        val cached = Some(CacheMap(testUtr, Map(
          tradingStartDetailsName -> Json.toJson(NewAWBusiness("Yes", Some(TupleDate("01", "01", "2014"))))
        )))

        await(testApplicationService.isNewBusiness(cached)) mustBe false
      }
    }
  }

  def testAddGroupRepToGroupMembers: (CacheMap,Option[GroupMembers]) = {
    val cached = cachedData()
    (cached, testApplicationService.addGroupRepToGroupMembers(Some(cached)))
  }

  def testReplaceGroupRepInGroupMembers: (CacheMap, Option[GroupMembers]) = {
    val cached = cachedData()
    val legalEntity = testLegalEntity.legalEntity.get
    val changed = CacheMap(testUtr, Map("legalEntity" -> Json.toJson(legalEntity),
      businessCustomerDetailsName -> Json.toJson(testBusinessNameChanged),
      businessNameDetailsName -> Json.toJson(testBusinessNameDetails()),
      tradingStartDetailsName -> Json.toJson(newAWBusiness()),
      businessRegistrationDetailsName -> Json.toJson(testBusinessRegistrationDetails(legalEntity)),
      placeOfBusinessName -> Json.toJson(testPlaceOfBusinessDefault()),
      groupMembersName -> Json.toJson(testGroupMemberDetails)))

    (cached, testApplicationService.replaceGroupRepInGroupMembers(Some(changed)))
  }

  def sendWithAuthorisedUser(test: Future[Either[SelfHealSubscriptionResponse, SuccessfulSubscriptionResponse]] => Any): Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockAWRSConnectorWithOnly(submitAWRSData = Right(subscribeSuccessResponse))
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    when(mockEmailService.sendConfirmationEmail(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
    val result = testApplicationService.sendApplication(TestUtil.defaultAuthRetrieval)
    test(result)
  }


  def sendWithAuthorisedUserSelfHeal(test: Future[Either[SelfHealSubscriptionResponse, SuccessfulSubscriptionResponse]] => Any): Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockAWRSConnectorWithOnly(submitAWRSData = Left(selfHealSuccessResponse))
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    when(mockEmailService.sendConfirmationEmail(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
    val result = testApplicationService.sendApplication(TestUtil.defaultAuthRetrieval)
    test(result)
  }

  def sendUpdateSubscriptionTypeWithAuthorisedUser(test: Future[SuccessfulSubscriptionResponse] => Any, newAW: Boolean = false): Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())

    val cachedSub = if (newAW) {
      val alteredBusinessDetails = cachedSubscription.businessDetails.map(bd => bd.copy(newAWBusiness = Some(NewAWBusiness("Yes", None))))
      cachedSubscription.copy(businessDetails = alteredBusinessDetails)
    } else {
      cachedSubscription
    }

    setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = cachedSub)
    setupMockAWRSConnectorWithOnly(updateAWRSData = subscribeUpdateSuccessResponse)
    implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    testApplicationService.updateApplication(TestUtil.defaultAuthRetrieval)
  }

  def sendCallUpdateGroupBusinessPartnerWithAuthorisedUser(test: Future[SuccessfulUpdateGroupBusinessPartnerResponse] => Any) : Unit = {
    setupMockSave4LaterService(fetchAll = cachedData())
    setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = cachedSubscription)
    setupMockAWRSConnectorWithOnly(updateGroupBusinessPartner = updateGroupBusinessPartnerResponse)
    FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> "test business")
    testApplicationService.callUpdateGroupBusinessPartner(cachedData(), Some(cachedSubscription), testSubscriptionStatusTypeApproved, TestUtil.defaultAuthRetrieval)
  }
}
