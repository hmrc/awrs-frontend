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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.GroupMemberDetailsForm._
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class GroupMemberDetailsFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form = GroupMemberDetailsForm.groupMemberForm.form

  "Form validation" should {

    "check validations for BusinessName and TradingName" in {
      NamedUnitTests.companyNamesAreValid(idPrefix = names, isBusinessNameRequired = true)
    }

    "display correct validation for group member address" in {
      NamedUnitTests.ukAddressIsCompulsoryAndValid(idPrefix = "address")
    }

    "display correct validation for group member VRN" in {
      NamedUnitTests.doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes()
    }

    "display correct validation for group member CRN" in {
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
      testForm.errors shouldBe Seq()
    }

    "display correct validation for group member UTR" in {
      val preCondition = Map[String, String]()
      val ignoreCondition = Set[Map[String, String]]()
      val idPrefix = None
      ProofOfIdentiticationVerifications.utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(
        preCondition,
        ignoreCondition,
        idPrefix,
        alsoTestWhenDoYouHaveUtrIsAnsweredWithNo = false)
    }

    "display correct validation for for at least one form of identification" in {
      val expectedError = ExpectedFieldIsEmpty("doYouHaveVRN", FieldError("awrs.generic.error.identification_provided"))
      val fieldIds = Set[String]("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR")
      NamedUnitTests.atLeastOneProofOfIdIsAnsweredWithYes(expectedError = expectedError, fieldIds = fieldIds)
    }

    "display correct validation for other group members question" in {
      val fieldId = "addAnotherGrpMember"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.group_member.addAnother.empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }
  }
}
