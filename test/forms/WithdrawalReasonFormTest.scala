/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.AWRSEnums.WithdrawalReasonEnum
import forms.AWRSEnums.WithdrawalReasonEnum._
import forms.test.util._
import forms.validation.util.FieldError
import models.WithdrawalReason
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig

class WithdrawalReasonFormTest extends PlaySpec with MockitoSugar  with AwrsFieldConfig with AwrsFormTestUtils {

  // lazy required when AwrsFieldConfig is used to make sure the app is started before the config is accessed
  implicit lazy val form: Form[WithdrawalReason] = WithdrawalReasonForm.withdrawalReasonForm.form
  val preDefinedTypes: Set[AWRSEnums.WithdrawalReasonEnum.Value] = Set(AppliedInError, NoLongerTrading, DuplicateApplication, JoinedAWRSGroup)

  "Withdrawal Reason Form" must {
    "Correctly validate that at least one selection is made" in {
      val fieldId = "withdrawalReason"
      val fieldName = "reason"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.withdrawal.error.reason_empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, WithdrawalReasonEnum)
      fieldName assertEnumFieldIsCompulsory expectations
    }

    "validate no radio buttons are selected" in {
      val fieldId = "reason"

      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.withdrawal.error.reason_empty"
        },
        _ => fail("Field should contain errors")
      )
    }
  }

  "validate content if 'Other' button selected " must {
    val conditionOtherSelected: Map[String, String] = Map("reason" -> Other.toString)
    val fieldId = "reasonOther"
    val fieldNameInErrorMessage = "other reasons why you are withdrawing your registration"

    "the other reasons field is left empty" in {
      form.bind(Map(fieldId -> "") ++ conditionOtherSelected).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.withdrawal.error.other.reason_empty"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the other reasons field maxLength is exceeded" in {
      form.bind(Map(fieldId -> "a" * 41) ++ conditionOtherSelected).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, withdrawalOtherReasonsLen)
        },
        _ => fail("Field should contain errors")
      )
    }

    "invalid characters are entered in the other reasons field" in {
      form.bind(Map(fieldId -> "α") ++ conditionOtherSelected).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }
  }
}
