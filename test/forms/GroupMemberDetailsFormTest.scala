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

package forms

import forms.AWRSEnums.BooleanRadioEnum
import forms.GroupMemberDetailsForm._
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class GroupMemberDetailsFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

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
      ProofOfIdentiticationVerifications.companyRegNumberIsValidWhenDoYouHaveCRNIsAnsweredWithYes(
        preCondition = Map(),
        ignoreCondition = Set(),
        doYouHaveCRNNameString = doYouHaveCrn,
        CRNNameString = "companyRegistrationNumber",
        crnPrefix = crnMapping,
        alsoTestWhenDoYouHaveCRNIsAnsweredWithNo = false
      )
      ProofOfIdentiticationVerifications.dateOfIncorporationIsCompulsoryAndValidWhenDoYouHaveCRNIsAnsweredWithYes(
        preCondition = Map(),
        ignoreCondition = Set(),
        idPrefix = crnMapping,
        doYouHaveCRNNameString = doYouHaveCrn
      )
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
