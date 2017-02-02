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

package forms

import forms.AWRSEnums.BooleanRadioEnum
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class WithdrawalConfirmationFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  implicit lazy val testForm = WithdrawalConfirmationForm.withdrawalConfirmation

  "Withdrawal Confirmation Form" should {
    "Correctly validate that at least one selection is made" in {
      val fieldId = "confirmation"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.withdrawal.confirmation.error.empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }
  }
}
