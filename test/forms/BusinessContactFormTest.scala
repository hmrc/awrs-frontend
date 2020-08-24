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
import forms.test.util._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.TestConstants._

class BusinessContactFormTest extends PlaySpec with MockitoSugar  with BeforeAndAfterEach with AwrsFormTestUtils {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val forms = BusinessContactsForm.businessContactsForm.form

  override def beforeEach(): Unit = {
    when(mockAppConfig.countryCodes)
      .thenReturn(mockCountryCodes)

    super.beforeEach()
  }

  "Business contacts form" must {

    f"check validations for contactAddress " in {
      val preCondition: Map[String, String] = Map("contactAddressSame" -> BooleanRadioEnum.No.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("contactAddressSame" -> BooleanRadioEnum.Yes.toString))
      val idPrefix: String = "contactAddress"

      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition, idPrefix, nameInErrorMessage = "")
    }

    "check validations for contactFirstName and contactLastName" in
      NamedUnitTests.firstNameAndLastNameIsCompulsoryAndValid(firstNameId = "contactFirstName", lastNameId = "contactLastName")

    "check validations for email" in {
      val goodData = Map(
        BusinessContactsForm.contactFirstName -> Seq("test"),
        BusinessContactsForm.contactLastName -> Seq("test"),
        BusinessContactsForm.telephone -> Seq("01234 567890"),
        BusinessContactsForm.email -> Seq("test@test.com"),
        BusinessContactsForm.contactAddressSame -> Seq(BooleanRadioEnum.Yes.toString)
      )

      val badData = Map(
        BusinessContactsForm.contactFirstName -> Seq("test"),
        BusinessContactsForm.contactLastName -> Seq("test"),
        BusinessContactsForm.telephone -> Seq("01234 567890"),
        BusinessContactsForm.email -> Seq("FAKE-EMAIL@test"),
        BusinessContactsForm.contactAddressSame -> Seq(BooleanRadioEnum.Yes.toString)
      )

      val bindedForm = forms.bindFromRequest(badData)
      bindedForm.errors.size mustBe 1

      val bindedFormGood = forms.bindFromRequest(goodData)
      bindedFormGood.errors mustBe Seq()
    }

    "check validations for telephone" in
      NamedUnitTests.fieldIsCompulsoryAndValid(fieldId = "telephone",
        emptyErrorMsg = "awrs.generic.error.telephone_empty",
        invalidFormatErrorMsg = "awrs.generic.error.telephone_numeric")

  }

  "Form validation" must {
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
