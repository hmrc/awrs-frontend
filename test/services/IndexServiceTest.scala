/*
 * Copyright 2017 HM Revenue & Customs
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

import _root_.models.{AdditionalBusinessPremisesList, _}
import builders.AuthBuilder
import controllers.auth.Utr._
import forms.AWRSEnums.BooleanRadioEnum
import play.api.test.FakeRequest
import services.DataCacheKeys._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._
import view_models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IndexServiceTest extends AwrsUnitTestTraits with ServicesUnitTestFixture {

  val request = FakeRequest()
  val mockDataCacheService = mock[Save4LaterService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupMockApplicationService()
  }

  object TestIndexService extends IndexService {
    override val dataCacheService = mockDataCacheService
    override val applicationService = mockApplicationService
  }

  "OptionUtil" should {
    import IndexService.OptionUtil

    "unsupported type should throw an exception" in {
      an[RuntimeException] should be thrownBy {
        Some(0).getOrElseSize
      }
    }

    "None is returned when No is answewred for suppliers and premises" in {
      val testAdditionalBusinessPremisesList = Some(
        AdditionalBusinessPremisesList(List(
          AdditionalBusinessPremises(additionalPremises =
            Some(BooleanRadioEnum.NoString),
            None,
            addAnother = None))))

      val testSuppliers = Some(Suppliers(List(
        Supplier(alcoholSuppliers = Some(BooleanRadioEnum.NoString),
          supplierName = None,
          vatRegistered = None,
          vatNumber = None,
          supplierAddress = None,
          additionalSupplier = None,
          ukSupplier = None)
      )))
      testAdditionalBusinessPremisesList.getOrElseSize shouldBe None
      testSuppliers.getOrElseSize shouldBe None
    }

    "return None when the input is None" in {
      val testGroupMemberDetails = None
      val testAdditionalBusinessPremisesList = None
      val testSuppliers = None
      val testPartnerDetails = None
      val testBusinessDirectors = None

      testGroupMemberDetails.getOrElseSize shouldBe None
      testAdditionalBusinessPremisesList.getOrElseSize shouldBe None
      testSuppliers.getOrElseSize shouldBe None
      testPartnerDetails.getOrElseSize shouldBe None
      testBusinessDirectors.getOrElseSize shouldBe None
    }

    "return None when there is nothing in the list" in {
      val testGroupMemberDetails = Some(GroupMembers(List()))
      val testAdditionalBusinessPremisesList = Some(AdditionalBusinessPremisesList(List()))
      val testSuppliers = Some(Suppliers(List()))
      val testPartnerDetails = Some(Partners(List()))
      val testBusinessDirectors = Some(BusinessDirectors(List[BusinessDirector]()))

      testGroupMemberDetails.getOrElseSize shouldBe None
      testAdditionalBusinessPremisesList.getOrElseSize shouldBe None
      testSuppliers.getOrElseSize shouldBe None
      testPartnerDetails.getOrElseSize shouldBe None
      testBusinessDirectors.getOrElseSize shouldBe None
    }

    "return the correct size when there is something in the list" in {
      val testGroupMemberDetails = Some(GroupMembers(List(testGroupMember, testGroupMember, testGroupMember)))
      val testAdditionalBusinessPremisesList = Some(AdditionalBusinessPremisesList(List(testAdditionalBusinessPremises, testAdditionalBusinessPremises, testAdditionalBusinessPremises)))
      val testSuppliers = Some(Suppliers(List(testSupplier(), testSupplier(), testSupplier())))
      val testPartnerDetails = Some(Partners(List(testPartner(), testPartner(), testPartner())))
      val testBusinessDirectors = Some(BusinessDirectors(List[BusinessDirector](testBusinessDirector, testBusinessDirector, testBusinessDirector)))

      testGroupMemberDetails.getOrElseSize.get shouldBe 3
      testAdditionalBusinessPremisesList.getOrElseSize.get shouldBe 3
      testSuppliers.getOrElseSize.get shouldBe 3
      testPartnerDetails.getOrElseSize.get shouldBe 3
      testBusinessDirectors.getOrElseSize.get shouldBe 3
    }
  }

  implicit class SectionModelTestUtil(sectionModelList: List[SectionModel]) {
    def getSection(sectionName: String, BusinessType: String): SectionModel =
      sectionModelList(JourneyConstants.getJourney(BusinessType).indexOf(sectionName))
  }

  implicit class IndexViewModelTestUtil(result: Future[IndexViewModel]) {
    def getSection(sectionName: String, BusinessType: String): SectionModel =
      await(result).sectionModels.getSection(sectionName, BusinessType)
  }

  "IndexService show continue button method " should {

    "return Not started for each section when an application is completely new, for any legal entity" in {
      testForNotStarted(legalEntityList)
      def testForNotStarted(legalEntity: List[String]): Unit = legalEntity match {
        case x :: Nil => testEachSectionStatusIsNotStarted(TestIndexService.getStatus(emptyCachemap, x).sectionModels)
        case x :: xs => testEachSectionStatusIsNotStarted(TestIndexService.getStatus(emptyCachemap, x).sectionModels); testForNotStarted(xs)
      }
      def testEachSectionStatusIsNotStarted(section: List[SectionModel]): Unit = section match {
        case x :: Nil => assert(x.status == SectionNotStarted)
        case x :: xs => assert(x.status == SectionNotStarted); testEachSectionStatusIsNotStarted(xs)
      }
    }

    "for all sections" should {
      allEntities.foreach {
        legalEntity =>
          s"return true for $legalEntity when all sections are completed" in {
            val result = TestIndexService.showContinueButton(TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity))
            assert(result)
          }
          s"return false for $legalEntity when all sections are NOT completed" in {
            val result = TestIndexService.showContinueButton(TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDetails = None), legalEntity))
            assert(!result)
          }
      }
    }
  }
  "IndexService getSection " should {

    "for business detail section" should {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses details are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDetails = None), legalEntity)
            result.getSection(businessDetailsName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessDetailsName, legalEntity).status shouldBe SectionEdited
          }
          s"return INCOMPLETE when $legalEntity New Business is true and proposed start date is missing" in {
            val result = TestIndexService.getStatus(testBusinessDetailsWithMissingStartDate(legalEntity = legalEntity, isNewBusiness = true), legalEntity)
            result.getSection(businessDetailsName, legalEntity).status shouldBe SectionIncomplete
          }
          s"return COMPLETE when $legalEntity New Business is false and proposed start date is missing" in {
            val temp = testBusinessDetailsWithMissingStartDate(legalEntity = legalEntity, isNewBusiness = false)
            val result = TestIndexService.getStatus(testBusinessDetailsWithMissingStartDate(legalEntity = legalEntity, isNewBusiness = false), legalEntity)
            result.getSection(businessDetailsName, legalEntity).status shouldBe SectionComplete
          }
      }
    }

    "for business registration detail section" should {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses registration details are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessRegistrationDetails = None), legalEntity)
            result.getSection(businessRegistrationDetailsName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity business registration details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessRegistrationDetailsName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity all business registration details exist" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessRegistrationDetailsName, legalEntity).status shouldBe SectionComplete
          }
      }
    }

    "for business contacts section" should {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses contacts are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessContacts = None), legalEntity)
            result.getSection(businessContactsName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity business contacts have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessContactsName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity all business contacts exist" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessContactsName, legalEntity).status shouldBe SectionComplete
          }
      }
    }

    "return for section Additional Premises" should {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Additional Premises are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = None), legalEntity)
            result.getSection(additionalBusinessPremisesName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Additional Premises have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(additionalBusinessPremisesName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity additionalPremises is NO" in {
            val businessPremises = AdditionalBusinessPremises(additionalPremises = Some("No"), Some(testAddress()), addAnother = Option("Yes"))
            val testAdditionalPremisesList = AdditionalBusinessPremisesList(List(businessPremises))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = testAdditionalPremisesList), legalEntity)
            result.getSection(additionalBusinessPremisesName, legalEntity).status shouldBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity additionalPremises is YES" in {
            val businessPremises = AdditionalBusinessPremises(additionalPremises = Some("Yes"), Some(testAddress()), addAnother = Option("Yes"))
            val testAdditionalPremisesList = AdditionalBusinessPremisesList(List(businessPremises))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = testAdditionalPremisesList), legalEntity)
            result.getSection(additionalBusinessPremisesName, legalEntity).status shouldBe SectionComplete
          }
      }
    }

    "return for section Business Directors" should {
      directorEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Business Directors are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = None), legalEntity)
            result.getSection(businessDirectorsName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Business Directors have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(businessDirectorsName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity otherDirectors is No" in {
            val testBusinessDirectors = BusinessDirectors(List(BusinessDirector(Some("Person"), firstName = Some("John"), lastName = Some("Smith"), doTheyHaveNationalInsurance = Option("Yes"), nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Option("Both"), otherDirectors = Some("No"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity)
            result.getSection(businessDirectorsName, legalEntity).status shouldBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity otherDirectors is Yes" in {
            val testBusinessDirectors = BusinessDirectors(List(BusinessDirector(Some("Person"), firstName = Some("John"), lastName = Some("Smith"), doTheyHaveNationalInsurance = Option("Yes"), nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Option("Both"), otherDirectors = Some("Yes"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity)
            result.getSection(businessDirectorsName, legalEntity).status shouldBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity not enough directors are added" in {
            val testBusinessDirectors = BusinessDirectors(Nil)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity)
            result.getSection(businessDirectorsName, legalEntity).status shouldBe SectionIncomplete
          }
      }
    }

    "return for section Business Partners" should {
      partnerEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Business partners details are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = None), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Business partners details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity 'other business partners to add' is No" in {
            val testBusinessPartnersList = Partners(List(testPartner(), testPartner(entityType = Some("Individual"), otherPartners = Some("No"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity 'other business partners to add' is Yes" in {
            val testBusinessPartnersList = Partners(List(testPartner(entityType = Some("Individual"), otherPartners = Some("Yes"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionIncomplete
          }
          s"return INCOMPLETE when $legalEntity not enough partners are added scenario 1" in {
            val testBusinessPartnersList = Partners(List(testPartner(entityType = Some("Individual"), otherPartners = Some("No"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionIncomplete
          }
          s"return INCOMPLETE when $legalEntity not enough partners are added scenario 2" in {
            val testBusinessPartnersList = Partners(Nil)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity)
            result.getSection(partnersName, legalEntity).status shouldBe SectionIncomplete
          }
      }
    }

    "return for section Group Members" should {
      groupEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Group Member details are not completed for LLP Group" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = None), legalEntity)
            result.getSection(groupMembersName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Group Member details have been changed for LTD Group" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(groupMembersName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE even when $legalEntity add additional group member is YES" in {
            val groupMembers = GroupMembers(List(GroupMember(CompanyNames(Some("ACME"), Some("Business1")), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Some("Yes"), testUtr, Some("No"), None, Some("No"), None, Some("Yes"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity)
            result.getSection(groupMembersName, legalEntity).status shouldBe SectionComplete
          }
          s"return COMPLETE when $legalEntity add additional group member is NO" in {
            val groupMembers = GroupMembers(List(GroupMember(CompanyNames(Some("ACME"), Some("Business1")), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Some("Yes"), testUtr, Some("No"), None, Some("No"), None, Some("No"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity)
            result.getSection(groupMembersName, legalEntity).status shouldBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity not enough group members are added" in {
            val groupMembers = GroupMembers(Nil)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity)
            result.getSection(groupMembersName, legalEntity).status shouldBe SectionIncomplete
          }
      }
    }

    "return for section Suppliers" should {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Suppliers are not completed" in {
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = None), legalEntity)
            result.getSection(suppliersName, legalEntity).status shouldBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Suppliers have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
            result.getSection(suppliersName, legalEntity).status shouldBe SectionEdited
          }
          s"return COMPLETE when $legalEntity alcoholSuppliers is No" in {
            val testSupplierAddressList = Suppliers(List(Supplier(alcoholSuppliers = Some("No"),
              supplierName = Some(""),
              vatRegistered = Some(""),
              vatNumber = Some(""),
              supplierAddress = Some(Address("", "", Some(" "), Some(""), Some(""))),
              additionalSupplier = Some(""),
              ukSupplier = Some(""))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = testSupplierAddressList), legalEntity)
            result.getSection(suppliersName, legalEntity).status shouldBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity additionalSupplier is Yes" in {
            val testSupplierAddressList = Suppliers(List(Supplier(alcoholSuppliers = Some("Yes"),
              supplierName = Some("Smith and co"),
              vatRegistered = Some("yes"),
              vatNumber = Some("123456789"),
              supplierAddress = Some(Address("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("NE12 2DS"))),
              additionalSupplier = Some("Yes"),
              ukSupplier = Some("No"))))
            val result = TestIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = testSupplierAddressList), legalEntity)
            result.getSection(suppliersName, legalEntity).status shouldBe SectionComplete
          }
      }
    }
  }

  private def checkNoDurationCacheResults(result: IndexViewModel, legalEntity: String) = {
    // business details is complete but business contact is not
    // n.b. these pages are always the first ones regardless of the entity type.
    result.sectionModels.getSection(businessDetailsName, legalEntity).status shouldBe SectionComplete
    result.sectionModels.getSection(businessRegistrationDetailsName, legalEntity).status shouldBe SectionComplete
    result.sectionModels.getSection(businessContactsName, legalEntity).status shouldBe SectionComplete
    result.sectionModels.getSection(placeOfBusinessName, legalEntity).status shouldBe SectionIncomplete
  }

  "IndexService should get status of all sections" should {
    allEntities.foreach {
      legalEntity =>
        s"return an IndexStatus model for business type $legalEntity" in {
          val result = TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity)
          await(result) shouldBe createIndexViewModel(legalEntity = legalEntity)
        }
        s"return an IndexStatus model for business type $legalEntity that is no longer complete" in {
          val completedCachemapApartFromNewBus = createCacheMap(legalEntity = legalEntity, businessDetails = (x: String) => testBusinessDetailsNoNewAWFlag)
          val result = TestIndexService.getStatus(completedCachemapApartFromNewBus, legalEntity)
          await(result) shouldBe createIndexViewModel(legalEntity = legalEntity, businessDetails = SectionIncomplete)
        }
        s"return INCOMPLETE status for business type $legalEntity (Operating duration is blank)" in {
          val noOpDurationCachemap = createCacheMap(legalEntity = legalEntity, placeOfBusiness = testPlaceOfBusinessNoOpDuration)
          val result = TestIndexService.getStatus(noOpDurationCachemap, legalEntity)
          checkNoDurationCacheResults(result, legalEntity)
        }
        s"return INCOMPLETE status when $legalEntity (postcode in main place of business is blank)" in {
          val noPostcodeMainPlaceOfBusinessAddressCachemap = createCacheMap(legalEntity = legalEntity, placeOfBusiness = testPlaceOfBusinessMainPlaceOfBusinessAddressNoPostcode)
          val result = TestIndexService.getStatus(noPostcodeMainPlaceOfBusinessAddressCachemap, legalEntity)
          result.sectionModels.getSection(placeOfBusinessName, legalEntity).status shouldBe SectionIncomplete
        }
    }
  }

  "IndexService " should {
    allEntities.foreach {
      legalEntity =>
        s"show continue button once all sections have status COMPLETE for business type $legalEntity" in {
          val result = TestIndexService.showContinueButton(TestIndexService.getStatus(createCacheMap(legalEntity), legalEntity))
          result shouldBe true
        }
        s"not show continue button if some sections have status NOT STARTED for $legalEntity" in {
          val incompleteDetails = createCacheMap(legalEntity = legalEntity, businessDetails = None)
          val result = TestIndexService.showContinueButton(TestIndexService.getStatus(incompleteDetails, legalEntity))
          result shouldBe false
        }
    }
    directorEntities.foreach {
      legalEntity =>
        s"not show continue button if some sections have status INCOMPLETE for $legalEntity" in {
          val incompleteDetails = createCacheMap(legalEntity = legalEntity, businessDirectors = BusinessDirectors(Nil))
          val result = TestIndexService.showContinueButton(TestIndexService.getStatus(incompleteDetails, legalEntity))
          result shouldBe false
        }
    }
  }

  "IndexService isCompleteForBusinessType helper method " should {
    "return false if an invalid legal entity is provided" in {
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), Some("Rubbish")) shouldBe false
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), None) shouldBe false
    }
    "return false if the legal entity provided is different to the legal entity stored in the business details entity" in {
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("SOP")) shouldBe false
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = None), Some("Partnership")) shouldBe false
    }
    "return true if the legal entity provided is different to the legal entity stored but in the same Identity category" in {
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("LLP")) shouldBe true
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LPP")), Some("LP")) shouldBe true
    }
    "return true if the Sole Trader section is complete" in {
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), Some("SOP")) shouldBe true
    }
    "return true if the Limited Company section is complete" in {
      TestIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("LTD")) shouldBe true
    }
  }
}
