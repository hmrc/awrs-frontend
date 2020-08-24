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

package services

import _root_.models.{AdditionalBusinessPremisesList, _}
import forms.AWRSEnums.BooleanRadioEnum
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.DataCacheKeys._
import utils.TestConstants._
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import view_models._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class IndexServiceTest extends AwrsUnitTestTraits with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val mockDataCacheService: Save4LaterService = mock[Save4LaterService]
  implicit val ec: ExecutionContext = mockMCC.executionContext

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupMockApplicationService()

    when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
      .thenReturn(true)
  }

  val testIndexService: IndexService = new IndexService(mockDataCacheService, mockApplicationService, mockAccountUtils)

  "OptionUtil" must {
    "unsupported type must throw an exception" in {
      an[RuntimeException] must be thrownBy {
        testIndexService.OptionUtil(Some(0)).getOrElseSize
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
      testIndexService.OptionUtil(testAdditionalBusinessPremisesList).getOrElseSize mustBe None
      testIndexService.OptionUtil(testSuppliers).getOrElseSize mustBe None
    }

    "return None when the input is None" in {
      val testGroupMemberDetails = None
      val testAdditionalBusinessPremisesList = None
      val testSuppliers = None
      val testPartnerDetails = None
      val testBusinessDirectors = None

      testIndexService.OptionUtil(testGroupMemberDetails).getOrElseSize mustBe None
      testIndexService.OptionUtil(testAdditionalBusinessPremisesList).getOrElseSize mustBe None
      testIndexService.OptionUtil(testSuppliers).getOrElseSize mustBe None
      testIndexService.OptionUtil(testPartnerDetails).getOrElseSize mustBe None
      testIndexService.OptionUtil(testBusinessDirectors).getOrElseSize mustBe None
    }

    "return None when there is nothing in the list" in {
      val testGroupMemberDetails = Some(GroupMembers(List()))
      val testAdditionalBusinessPremisesList = Some(AdditionalBusinessPremisesList(List()))
      val testSuppliers = Some(Suppliers(List()))
      val testPartnerDetails = Some(Partners(List()))
      val testBusinessDirectors = Some(BusinessDirectors(List[BusinessDirector]()))

      testIndexService.OptionUtil(testGroupMemberDetails).getOrElseSize mustBe None
      testIndexService.OptionUtil(testAdditionalBusinessPremisesList).getOrElseSize mustBe None
      testIndexService.OptionUtil(testSuppliers).getOrElseSize mustBe None
      testIndexService.OptionUtil(testPartnerDetails).getOrElseSize mustBe None
      testIndexService.OptionUtil(testBusinessDirectors).getOrElseSize mustBe None
    }

    "return the correct size when there is something in the list" in {
      val testGroupMemberDetails = Some(GroupMembers(List(testGroupMember, testGroupMember, testGroupMember)))
      val testAdditionalBusinessPremisesList = Some(AdditionalBusinessPremisesList(List(testAdditionalBusinessPremises, testAdditionalBusinessPremises, testAdditionalBusinessPremises)))
      val testSuppliers = Some(Suppliers(List(testSupplier(), testSupplier(), testSupplier())))
      val testPartnerDetails = Some(Partners(List(testPartner(), testPartner(), testPartner())))
      val testBusinessDirectors = Some(BusinessDirectors(List[BusinessDirector](testBusinessDirector, testBusinessDirector, testBusinessDirector)))

      testIndexService.OptionUtil(testGroupMemberDetails).getOrElseSize.get mustBe 3
      testIndexService.OptionUtil(testAdditionalBusinessPremisesList).getOrElseSize.get mustBe 3
      testIndexService.OptionUtil(testSuppliers).getOrElseSize.get mustBe 3
      testIndexService.OptionUtil(testPartnerDetails).getOrElseSize.get mustBe 3
      testIndexService.OptionUtil(testBusinessDirectors).getOrElseSize.get mustBe 3
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

  "IndexService show continue button method " must {

    "return Not started for each section when an application is completely new, for any legal entity" in {
      testForNotStarted(legalEntityList)
      @tailrec
      def testForNotStarted(legalEntity: List[String]): Unit = legalEntity match {
        case x :: Nil => testEachSectionStatusIsNotStarted(
          await(testIndexService.getStatus(emptyCachemap, x, TestUtil.defaultAuthRetrieval)).sectionModels)
        case x :: xs => testEachSectionStatusIsNotStarted(
          await(testIndexService.getStatus(emptyCachemap, x, TestUtil.defaultAuthRetrieval)).sectionModels); testForNotStarted(xs)
        case Nil => Unit
      }
      @tailrec
      def testEachSectionStatusIsNotStarted(section: List[SectionModel]): Unit = section match {
        case x :: Nil => assert(x.status == SectionNotStarted)
        case x :: xs => assert(x.status == SectionNotStarted); testEachSectionStatusIsNotStarted(xs)
        case _ => Unit
      }
    }

    "for all sections" must {
      allEntities.foreach {
        legalEntity =>
          s"return true for $legalEntity when all sections are completed" in {
            val result = testIndexService.showContinueButton(await(testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)))
            assert(result)
          }
          s"return false for $legalEntity when all sections are NOT completed" in {
            val result = testIndexService.showContinueButton(await(testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessNameDetails = None), legalEntity, TestUtil.defaultAuthRetrieval)))
            assert(!result)
          }
      }
    }
  }
  "IndexService getSection " must {

    "for business detail section" must {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses details are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessNameDetails = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDetailsName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDetailsName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity New Business is false and proposed start date is missing" in {
            testBusinessDetailsWithMissingStartDate(legalEntity = legalEntity, isNewBusiness = false)
            val result = testIndexService.getStatus(testBusinessDetailsWithMissingStartDate(legalEntity = legalEntity, isNewBusiness = false, propDate = Some(TupleDate("20", "1", "2019"))), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDetailsName, legalEntity).status mustBe SectionComplete
          }
      }
    }

    "for business registration detail section" must {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses registration details are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessRegistrationDetails = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessRegistrationDetailsName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity business registration details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessRegistrationDetailsName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity all business registration details exist" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessRegistrationDetailsName, legalEntity).status mustBe SectionComplete
          }
      }
    }

    "for business contacts section" must {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity businesses contacts are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessContacts = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessContactsName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity business contacts have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessContactsName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity all business contacts exist" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessContactsName, legalEntity).status mustBe SectionComplete
          }
      }
    }

    "return for section Additional Premises" must {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Additional Premises are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(additionalBusinessPremisesName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Additional Premises have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(additionalBusinessPremisesName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity additionalPremises is NO" in {
            val businessPremises = AdditionalBusinessPremises(additionalPremises = Some("No"), Some(testAddress()), addAnother = Option("Yes"))
            val testAdditionalPremisesList = AdditionalBusinessPremisesList(List(businessPremises))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = testAdditionalPremisesList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(additionalBusinessPremisesName, legalEntity).status mustBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity additionalPremises is YES" in {
            val businessPremises = AdditionalBusinessPremises(additionalPremises = Some("Yes"), Some(testAddress()), addAnother = Option("Yes"))
            val testAdditionalPremisesList = AdditionalBusinessPremisesList(List(businessPremises))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, additionalBusinessPremises = testAdditionalPremisesList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(additionalBusinessPremisesName, legalEntity).status mustBe SectionComplete
          }
      }
    }

    "return for section Business Directors" must {
      directorEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Business Directors are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDirectorsName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Business Directors have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDirectorsName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity otherDirectors is No" in {
            val testBusinessDirectors = BusinessDirectors(List(BusinessDirector(Some("Person"), firstName = Some("John"), lastName = Some("Smith"), doTheyHaveNationalInsurance = Option("Yes"), nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Option("Both"), otherDirectors = Some("No"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDirectorsName, legalEntity).status mustBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity otherDirectors is Yes" in {
            val testBusinessDirectors = BusinessDirectors(List(BusinessDirector(Some("Person"), firstName = Some("John"), lastName = Some("Smith"), doTheyHaveNationalInsurance = Option("Yes"), nino = testNino, passportNumber = None, nationalID = None, companyNames = None, doYouHaveUTR = None, utr = None, doYouHaveCRN = None, companyRegNumber = None, doYouHaveVRN = None, vrn = None, directorsAndCompanySecretaries = Option("Both"), otherDirectors = Some("Yes"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDirectorsName, legalEntity).status mustBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity not enough directors are added" in {
            val testBusinessDirectors = BusinessDirectors(Nil)
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, businessDirectors = testBusinessDirectors), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(businessDirectorsName, legalEntity).status mustBe SectionIncomplete
          }
      }
    }

    "return for section Business Partners" must {
      partnerEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Business partners details are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Business partners details have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity 'other business partners to add' is No" in {
            val testBusinessPartnersList = Partners(List(testPartner(), testPartner(entityType = Some("Individual"), otherPartners = Some("No"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity 'other business partners to add' is Yes" in {
            val testBusinessPartnersList = Partners(List(testPartner(entityType = Some("Individual"), otherPartners = Some("Yes"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionIncomplete
          }
          s"return INCOMPLETE when $legalEntity not enough partners are added scenario 1" in {
            val testBusinessPartnersList = Partners(List(testPartner(entityType = Some("Individual"), otherPartners = Some("No"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionIncomplete
          }
          s"return INCOMPLETE when $legalEntity not enough partners are added scenario 2" in {
            val testBusinessPartnersList = Partners(Nil)
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, partnerDetails = testBusinessPartnersList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(partnersName, legalEntity).status mustBe SectionIncomplete
          }
      }
    }

    "return for section Group Members" must {
      groupEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Group Member details are not completed for LLP Group" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(groupMembersName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Group Member details have been changed for LTD Group" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(groupMembersName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE even when $legalEntity add additional group member is YES" in {
            val groupMembers = GroupMembers(List(GroupMember(CompanyNames(Some("ACME"), Some("Business1")), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Some("Yes"), testUtr, Some("No"), None, Some("No"), None, Some("Yes"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(groupMembersName, legalEntity).status mustBe SectionComplete
          }
          s"return COMPLETE when $legalEntity add additional group member is NO" in {
            val groupMembers = GroupMembers(List(GroupMember(CompanyNames(Some("ACME"), Some("Business1")), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Some("Yes"), testUtr, Some("No"), None, Some("No"), None, Some("No"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(groupMembersName, legalEntity).status mustBe SectionComplete
          }
          s"return INCOMPLETE when $legalEntity not enough group members are added" in {
            val groupMembers = GroupMembers(Nil)
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, groupMemberDetails = groupMembers), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(groupMembersName, legalEntity).status mustBe SectionIncomplete
          }
      }
    }

    "return for section Suppliers" must {
      allEntities.foreach {
        legalEntity =>
          s"return NOT STARTED when $legalEntity Suppliers are not completed" in {
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = None), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(suppliersName, legalEntity).status mustBe SectionNotStarted
          }
          s"return EDITED when $legalEntity Suppliers have been changed" in {
            setupMockApplicationService(getApi5ChangeIndicators = getSectionChangeIndicatorsAllTrue)
            val result = testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(suppliersName, legalEntity).status mustBe SectionEdited
          }
          s"return COMPLETE when $legalEntity alcoholSuppliers is No" in {
            val testSupplierAddressList = Suppliers(List(Supplier(alcoholSuppliers = Some("No"),
              supplierName = Some(""),
              vatRegistered = Some(""),
              vatNumber = Some(""),
              supplierAddress = Some(Address("", "", Some(" "), Some(""), Some(""))),
              additionalSupplier = Some(""),
              ukSupplier = Some(""))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = testSupplierAddressList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(suppliersName, legalEntity).status mustBe SectionComplete
          }
          s"return COMPLETE even when $legalEntity additionalSupplier is Yes" in {
            val testSupplierAddressList = Suppliers(List(Supplier(alcoholSuppliers = Some("Yes"),
              supplierName = Some("Smith and co"),
              vatRegistered = Some("yes"),
              vatNumber = Some("123456789"),
              supplierAddress = Some(Address("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("NE12 2DS"))),
              additionalSupplier = Some("Yes"),
              ukSupplier = Some("No"))))
            val result = testIndexService.getStatus(createCacheMap(legalEntity = legalEntity, suppliers = testSupplierAddressList), legalEntity, TestUtil.defaultAuthRetrieval)
            result.getSection(suppliersName, legalEntity).status mustBe SectionComplete
          }
      }
    }
  }

  private def checkNoDurationCacheResults(result: IndexViewModel, legalEntity: String) = {
    // business details is complete but business contact is not
    // n.b. these pages are always the first ones regardless of the entity type.
    result.sectionModels.getSection(businessDetailsName, legalEntity).status mustBe SectionComplete
    result.sectionModels.getSection(businessRegistrationDetailsName, legalEntity).status mustBe SectionComplete
    result.sectionModels.getSection(businessContactsName, legalEntity).status mustBe SectionComplete
    result.sectionModels.getSection(placeOfBusinessName, legalEntity).status mustBe SectionIncomplete
  }

  val startDate = Some(TupleDate("20", "1", "2019"))

  "IndexService must get status of all sections" must {
    allEntities.foreach {
      legalEntity =>
        s"return an IndexStatus model for business type $legalEntity" in {
          val result = testIndexService.getStatus(createCacheMap(legalEntity, tradingStartDetails = newAWBusiness().copy(proposedStartDate = startDate)), legalEntity, TestUtil.defaultAuthRetrieval)
          await(result) mustBe createIndexViewModel(legalEntity = legalEntity)
        }
        s"return an IndexStatus model for business type $legalEntity that is no longer complete" in {
          val completedCachemapApartFromNewBus = createCacheMap(legalEntity = legalEntity, businessNameDetails = (x: String) => testBusinessNameDetails(), businessDirectors = testBusinessDirectors.copy(directors = Nil))
          val result = testIndexService.getStatus(completedCachemapApartFromNewBus, legalEntity, TestUtil.defaultAuthRetrieval)
          await(result) mustBe createIndexViewModel(legalEntity = legalEntity, businessDirectors = SectionIncomplete, directorSize = 0)
        }
        s"return INCOMPLETE status for business type $legalEntity (Operating duration is blank)" in {
          val noOpDurationCachemap = createCacheMap(legalEntity = legalEntity, placeOfBusiness = testPlaceOfBusinessNoOpDuration)
          val result = testIndexService.getStatus(noOpDurationCachemap, legalEntity, TestUtil.defaultAuthRetrieval)
          checkNoDurationCacheResults(await(result), legalEntity)
        }
        s"return INCOMPLETE status when $legalEntity (postcode in main place of business is blank)" in {
          val noPostcodeMainPlaceOfBusinessAddressCachemap = createCacheMap(legalEntity = legalEntity, placeOfBusiness = testPlaceOfBusinessMainPlaceOfBusinessAddressNoPostcode)
          val result = testIndexService.getStatus(noPostcodeMainPlaceOfBusinessAddressCachemap, legalEntity, TestUtil.defaultAuthRetrieval)
          await(result).sectionModels.getSection(placeOfBusinessName, legalEntity).status mustBe SectionIncomplete
        }
    }
  }

  "IndexService " must {
    allEntities.foreach {
      legalEntity =>
        s"show continue button once all sections have status COMPLETE for business type $legalEntity" in {
          val result = testIndexService.showContinueButton(await(testIndexService.getStatus(createCacheMap(legalEntity), legalEntity, TestUtil.defaultAuthRetrieval)))
          result mustBe true
        }
        s"not show continue button if some sections have status NOT STARTED for $legalEntity" in {
          val incompleteDetails = createCacheMap(legalEntity = legalEntity, businessNameDetails = None)
          val result = testIndexService.showContinueButton(await(testIndexService.getStatus(incompleteDetails, legalEntity, TestUtil.defaultAuthRetrieval)))
          result mustBe false
        }
    }
    directorEntities.foreach {
      legalEntity =>
        s"not show continue button if some sections have status INCOMPLETE for $legalEntity" in {
          val incompleteDetails = createCacheMap(legalEntity = legalEntity, businessDirectors = BusinessDirectors(Nil))
          val result = testIndexService.showContinueButton(await(testIndexService.getStatus(incompleteDetails, legalEntity, TestUtil.defaultAuthRetrieval)))
          result mustBe false
        }
    }
  }

  "IndexService isCompleteForBusinessType helper method " must {
    "return false if an invalid legal entity is provided" in {
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), Some("Rubbish")) mustBe false
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), None) mustBe false
    }
    "return false if the legal entity provided is different to the legal entity stored in the business details entity" in {
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("SOP")) mustBe false
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = None), Some("Partnership")) mustBe false
    }
    "return true if the legal entity provided is different to the legal entity stored but in the same Identity category" in {
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("LLP")) mustBe true
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LPP")), Some("LP")) mustBe true
    }
    "return true if the Sole Trader section is complete" in {
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(), Some("SOP")) mustBe true
    }
    "return true if the Limited Company section is complete" in {
      testIndexService.displayCompleteForBusinessType(testBusinessRegistrationDetails(legalEntity = Some("LTD")), Some("LTD")) mustBe true
    }
  }
}
