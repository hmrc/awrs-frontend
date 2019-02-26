/*
 * Copyright 2019 HM Revenue & Customs
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
import forms.validation.util.{FieldError, SummaryError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.views.html.helpers.form
import utils.TestConstants._

class BusinessContactFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  implicit lazy val forms = BusinessContactsForm.businessContactsForm.form

  "Business contacts form" should {

    f"check validations for contactAddress " in {
      val preCondition: Map[String, String] = Map("contactAddressSame" -> BooleanRadioEnum.No.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("contactAddressSame" -> BooleanRadioEnum.Yes.toString))
      val idPrefix: String = "contactAddress"

      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition, idPrefix, nameInErrorMessage = "")
    }

    "check validations for contactFirstName and contactLastName" in
      NamedUnitTests.firstNameAndLastNameIsCompulsoryAndValid(firstNameId = "contactFirstName", lastNameId = "contactLastName")

    "check validations for email" in
      NamedUnitTests.feildIsCompulsoryAndValid(fieldId = "email",
        emptyErrorMsg = "awrs.generic.error.email_empty",
        invalidFormatErrorMsg = "awrs.generic.error.email_invalid")

    "check validations for telephone" in
      NamedUnitTests.feildIsCompulsoryAndValid(fieldId = "telephone",
        emptyErrorMsg = "awrs.generic.error.telephone_empty",
        invalidFormatErrorMsg = "awrs.generic.error.telephone_numeric")

  }

  "Form validation" should {
    "check Welsh character validations for First Name, Last Name and Contact Address" in {
      val data: Map[String, String] =
        Map("contactAddressSame" -> "No",
          "contactAddress.addressLine1" -> testWelshChars,
          "contactAddress.addressLine2" -> testWelshChars,
          "contactAddress.addressLine3" -> testWelshChars,
          "contactAddress.addressLine4" -> testWelshChars,
          "contactAddress.postcode" -> testPostcode,
          "contactFirstName" -> testWelshChars,
          "contactLastName" -> testWelshChars,
          "email" -> "test@test.com",
          "telephone" -> "01912244194"
        )
      assertFormIsValid(forms, data)
    }
  }
}
