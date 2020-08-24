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

package forms

import forms.AWRSEnums.BooleanRadioEnum
import forms.BusinessRegistrationDetailsForm._
import forms.test.util.{ExpectedFieldIsEmpty, _}
import forms.validation.util.FieldError
import models.BusinessRegistrationDetails
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import utils.TestConstants._
import utils.TestUtil

class BusinessRegistrationDetailsFormTest extends PlaySpec with MockitoSugar  with AwrsFormTestUtils {
  lazy val forms = (entity: String) => businessRegistrationDetailsForm(entity).form
  val SoleTrader = "SOP"
  val Ltd = "LTD"
  val Partnership = "Partnership"
  val entities = Seq[String](SoleTrader, Ltd, Partnership)

  //all ids
  Set[String](doYouHaveUtr, doYouHaveNino, doYouHaveCrn, doYouHaveVrn)

  // ids required by entities
  private val soleTraderIds = Set[String](doYouHaveUtr, doYouHaveNino, doYouHaveVrn)
  private val limitedIds = Set[String](doYouHaveUtr, doYouHaveCrn, doYouHaveVrn)
  private val partnership = Set[String](doYouHaveUtr, doYouHaveVrn)


  type TestData = Map[String, String]

  private val crnField = s"$crnMapping.companyRegistrationNumber"

  private val validData: Map[String, TestData] = Map(
    doYouHaveUtr -> Map((utr, testUtr)),
    doYouHaveNino -> Map((nino, testNino)),
    doYouHaveCrn ->
      Map((crnField, testCrn),
        (s"$crnMapping.dateOfIncorporation.day", "01"),
        (s"$crnMapping.dateOfIncorporation.month", "01"),
        (s"$crnMapping.dateOfIncorporation.year", "2015")),
    doYouHaveVrn -> Map((doYouHaveVrn, BooleanRadioEnum.YesString), (vrn, testVrn))
  )

  private val expectedEmpty: Map[String, ExpectedFieldIsEmpty] = Map(
    (doYouHaveUtr, ExpectedFieldIsEmpty(utr, FieldError("awrs.generic.error.utr_empty"))),
    (doYouHaveNino, ExpectedFieldIsEmpty(nino, FieldError("awrs.generic.error.nino_empty"))),
    (doYouHaveCrn, ExpectedFieldIsEmpty(crnField, FieldError("awrs.generic.error.companyRegNumber_empty"))),
    (doYouHaveVrn, ExpectedFieldIsEmpty(vrn, FieldError("awrs.generic.error.vrn_empty")))
  )
  private val expectedEmptyForUTR: Map[String, ExpectedFieldIsEmpty] = Map(
    (doYouHaveUtr, ExpectedFieldIsEmpty(utr, FieldError("awrs.generic.error.utr_empty_LTD")))
  )

  private val doYouHaveToTargetField = Map(
    (doYouHaveUtr, utr),
    (doYouHaveNino, nino),
    (doYouHaveCrn, crnField),
    (doYouHaveVrn, vrn)
  )

  "Business registration details form" must {
    entities.foreach { entity =>
      implicit lazy val form = forms(entity)

      lazy val targetIds: Set[String] = entity match {
        case SoleTrader => soleTraderIds
        case Ltd => limitedIds
        case Partnership => partnership
      }

      /*
      *  Custom validation tests are used for Utr, Nino and Crn because they no longer receive the "do you have" questions
      *  from the front end. The answers to these questions are instead inferred by the form based on whether or not the
      *  user had supplied these fields.
      *
      *  The tests still make use of the "do you have" question fields as these will be populated by the inferrence in
      *  the preprocessor
      */

      s"check the validations of utr for entity: $entity" in {
        val preCondition = Map[String, String]()
        val ignoreCondition = Set[Map[String, String]]()
        val idPrefix = None
        val isEntityLTD = if(entity == "LTD") true else false
        ProofOfIdentiticationVerifications.utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(
          preCondition,
          ignoreCondition,
          idPrefix,
          alsoTestWhenDoYouHaveUtrIsAnsweredWithNo = false,
          isLTD = isEntityLTD)
      }

      s"check the validations of nino for entity: $entity" in {
        targetIds.contains(doYouHaveNino) match {
          case false =>
          case true =>
            val preCondition = Map[String, String]()
            val ignoreCondition = Set[Map[String, String]]()
            val idPrefix = None
            val doYouHaveNinoNameString = doYouHaveNino
            ProofOfIdentiticationVerifications.ninoIsValidatedIfHaveNinoIsYes(
              preCondition,
              ignoreCondition,
              idPrefix,
              doYouHaveNinoNameString,
              alsoTestWhenDoYouHaveNinoIsAnsweredWithNo = false)
        }
      }

      s"check the validations of crn for entity: $entity" in {
        if (targetIds.contains(doYouHaveCrn)) {
          val data = Map(
            "companyNames.businessName" -> Seq("Business Name"),
            "companyNames.doYouHaveTradingName" -> Seq("No"),
            "address.addressLine1" -> Seq("1 Testing Test Road"),
            "address.addressLine2" -> Seq("Testton"),
            "address.postcode" -> Seq("NE98 1ZZ"),
            "doYouHaveUTR" -> Seq("No"),
            "isBusinessIncorporated" -> Seq("Yes"),
            "companyRegDetails.companyRegistrationNumber" -> Seq("10101010"),
            "companyRegDetails.dateOfIncorporation.day" -> Seq("20"),
            "companyRegDetails.dateOfIncorporation.month" -> Seq("5"),
            "companyRegDetails.dateOfIncorporation.year" -> Seq("2015"),
            "doYouHaveVRN" -> Seq("No"),
            "addAnotherGrpMember" -> Seq("No")
          )

          val testForm = form.bindFromRequest(data)
          testForm.errors mustBe Seq()
        } else {

        }
      }

      s"check the validations of vrn for entity: $entity" in {
        NamedUnitTests.doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes()
      }

      s"at if the user chose no to doYouHaveVrn then the other ids will prompt with the empty error message for entity: $entity" in {
        val testData: TestData = targetIds.map((x: String) => (x, BooleanRadioEnum.NoString)).toMap
        val formWithErrors = forms(entity).bind(testData)

        def mainTest(doYouHaveId: String) = {


          val fieldId = doYouHaveToTargetField(doYouHaveId)
          formWithErrors.hasErrors mustBe true

          if( doYouHaveId == "doYouHaveUTR" && entity == "LTD"){
            val expected = expectedEmptyForUTR(doYouHaveId)
            assertFieldError(formWithErrors, fieldId, expected.fieldError)
            assertSummaryError(formWithErrors, fieldId, expected.summaryError)
          }
          else{
            val expected = expectedEmpty(doYouHaveId)
            assertFieldError(formWithErrors, fieldId, expected.fieldError)
            assertSummaryError(formWithErrors, fieldId, expected.summaryError)
          }



        }
        targetIds.foreach {
          case `doYouHaveVrn` => // the user can have No as an answer to VRN in this new format
          case `doYouHaveCrn` => // in case of CRN also check for date of incorporation
            mainTest(doYouHaveCrn)
            val dateOfIncorporation = s"$crnMapping.dateOfIncorporation"
            val expected = ExpectedFieldIsEmpty(dateOfIncorporation, FieldError("awrs.generic.error.companyRegDate_empty"))
            assertFieldError(formWithErrors, dateOfIncorporation, expected.fieldError)
            assertSummaryError(formWithErrors, dateOfIncorporation, expected.summaryError)
          case doYouHaveId => mainTest(doYouHaveId)
        }
      }

      s"A valid form only required a single applicable ID for entity: $entity" in {
        val doYouHaveQuestionsPreReq: TestData = targetIds.map((x: String) => (x, BooleanRadioEnum.NoString)).toMap
        validData.foreach {
          case (id: String, validId: TestData) =>
            targetIds.contains(id) match {
              case false => // ignore this test since the id is not present in the requirement
              case true =>
                val overwriteDoYouHave: TestData = Map((id, BooleanRadioEnum.YesString))
                val testData: TestData = doYouHaveQuestionsPreReq ++ overwriteDoYouHave ++ validId
                assertFormIsValid(forms(entity), testData)
            }
        }
      }

      s"A valid form can consists of all Ids applicable for entity: $entity" in {
        val doYouHaveQuestionsPreReq: TestData = targetIds.map((x: String) => (x, BooleanRadioEnum.YesString)).toMap
        val testData = doYouHaveQuestionsPreReq ++ targetIds.map(x => validData(x)).reduce(_ ++ _)
        assertFormIsValid(forms(entity), testData)
      }

    }
  }

  "Business registration details form's preprocessor" must {
    val onlyVRN = (legalEntity: String) => BusinessRegistrationDetails(
      legalEntity = Some(legalEntity),
      doYouHaveUTR = None,
      utr = None,
      doYouHaveNino = None,
      nino = None,
      isBusinessIncorporated = None,
      companyRegDetails = None,
      doYouHaveVRN = Some(BooleanRadioEnum.YesString),
      vrn = Some(testVrn)
    )

    lazy val onlyUTR = (legalEntity: String) => BusinessRegistrationDetails(
      legalEntity = Some(legalEntity),
      doYouHaveUTR = None,
      utr = Some(testUtr),
      doYouHaveNino = None,
      nino = Some(testNino),
      isBusinessIncorporated = None,
      companyRegDetails = Some(TestUtil.testCompanyRegDetails()),
      doYouHaveVRN = Some(BooleanRadioEnum.NoString),
      vrn = None
    )

    def testForm(entityType: String)(testData: BusinessRegistrationDetails)(test: (BusinessRegistrationDetails) => Unit) = {
      val fakeRequest = FakeRequest()
      val preProcessedForm = businessRegistrationDetailsForm(entityType)
      val request = TestUtil.populateFakeRequest(fakeRequest, preProcessedForm.form, testData)
      preProcessedForm.bindFromRequest()(request).fold(
        formWithErrors => {
          withClue("This form must have been valid") {
            true mustBe false
          }
        },
        valid => test(valid)
      )
    }

    def testId(formHasId: Boolean)(field: Option[String])(expected: String) = formHasId match {
      case true => field mustBe Some(expected)
      case false => field mustBe None // if formHasId is false then this field must not be populated
    }

    entities.foreach {
      entityType =>
        val expectedIds = entityType match {
          case SoleTrader => soleTraderIds
          case Ltd => limitedIds
          case Partnership => partnership
        }
        s"correctly infer the answers to the do you have questions for the ids for $entityType" in {
          testForm(entityType)(onlyVRN(entityType)) { valid =>
            testId(expectedIds.contains(doYouHaveUtr))(valid.doYouHaveUTR)(BooleanRadioEnum.NoString)
            testId(expectedIds.contains(doYouHaveNino))(valid.doYouHaveNino)(BooleanRadioEnum.NoString)
            testId(expectedIds.contains(doYouHaveCrn))(valid.isBusinessIncorporated)(BooleanRadioEnum.NoString)
          }
          testForm(entityType)(onlyUTR(entityType)) { valid =>
            testId(expectedIds.contains(doYouHaveUtr))(valid.doYouHaveUTR)(BooleanRadioEnum.YesString)
            testId(expectedIds.contains(doYouHaveNino))(valid.doYouHaveNino)(BooleanRadioEnum.YesString)
            testId(expectedIds.contains(doYouHaveCrn))(valid.isBusinessIncorporated)(BooleanRadioEnum.YesString)
          }
        }
    }

  }

}
