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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.test.util._
import models.BusinessContacts
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig
import utils.TestConstants._

class BusinessContactFormTest extends PlaySpec with MockitoSugar  with BeforeAndAfterEach with AwrsFormTestUtils with AwrsFieldConfig {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form: Form[BusinessContacts] = BusinessContactsForm.businessContactsForm.form

  override def beforeEach(): Unit = {
    when(mockAppConfig.countryCodes)
      .thenReturn(mockCountryCodes)

    super.beforeEach()
  }

  "Business contacts form" must {


    "check validations for Address Lines" when {
      val prefix = "contactAddress"
      val fieldNameInErrorMessage = "address line"
      val fieldNameInErrorMessagePostcode = "postcode"
      val preCondition: Map[String, String] = Map("contactAddressSame" -> BooleanRadioEnum.No.toString)

      "the fields are left empty" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "",
          s"$prefix.addressLine4" -> "",
          s"$prefix.postcode" -> ""
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe "awrs.generic.error.addressLine1_empty"
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe "awrs.generic.error.addressLine2_empty"
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe "awrs.generic.error.postcode_empty"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "a" * 36,
          s"$prefix.addressLine2" -> "a" * 36,
          s"$prefix.addressLine3" -> "a" * 36,
          s"$prefix.addressLine4" -> "a" * 36,
          s"$prefix.postcode" -> "a" * 21
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
          },
          _ => fail("Field should contain errors")
        )
      }

      "invalid characters are entered in the field" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "α",
          s"$prefix.addressLine2" -> "α",
          s"$prefix.addressLine3" -> "α",
          s"$prefix.addressLine4" -> "α",
          s"$prefix.postcode" -> "α"
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "check 'first name' and 'last name' validation" when {

      val fieldIdFirstName = "contactFirstName"
      val fieldIdSurname = "contactLastName"
      val fieldNameInErrorMessageFN = "first name"
      val fieldNameInErrorMessageLN = "last name"

      "the first name field is left empty" in {
        form.bind(Map(fieldIdFirstName -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldIdFirstName).errors.size mustBe 1
            formWithErrors(fieldIdFirstName).errors.head.message mustBe "awrs.generic.error.first_name_empty"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the last name field is left empty" in {
        form.bind(Map(fieldIdSurname -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldIdSurname).errors.size mustBe 1
            formWithErrors(fieldIdSurname).errors.head.message mustBe "awrs.generic.error.last_name_empty"
          },
          _ => fail("Field should contain errors")
        )
      }

//      "the first name field maxLength is exceeded" in {
//        form.bind(Map(fieldIdFirstName -> "a" * 36)).fold(
//          formWithErrors => {
//            formWithErrors(fieldIdFirstName).errors.size mustBe 1
//            messages(formWithErrors(fieldIdFirstName).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessageFN, firstNameLen)
//          },
//          _ => fail("Field should contain errors")
//        )
//      }
//
//      "the last name field maxLength is exceeded" in {
//        form.bind(Map(fieldIdSurname -> "a" * 36)).fold(
//          formWithErrors => {
//            formWithErrors(fieldIdSurname).errors.size mustBe 1
//            messages(formWithErrors(fieldIdSurname).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessageLN, lastNameLen)
//          },
//          _ => fail("Field should contain errors")
//        )
//      }

      "invalid characters are entered in the first name field" in {
        form.bind(Map(fieldIdFirstName -> "α")).fold(
          formWithErrors => {
            formWithErrors(fieldIdFirstName).errors.size mustBe 1
            messages(formWithErrors(fieldIdFirstName).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessageFN)
          },
          _ => fail("Field should contain errors")
        )
      }

      "invalid characters are entered in the last name field" in {
        form.bind(Map(fieldIdSurname -> "α")).fold(
          formWithErrors => {
            formWithErrors(fieldIdSurname).errors.size mustBe 1
            messages(formWithErrors(fieldIdSurname).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessageLN)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

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

      val bindedForm = form.bindFromRequest(badData)
      bindedForm.errors.size mustBe 1

      val bindedFormGood = form.bindFromRequest(goodData)
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
      assertFormIsValid(form, data)
    }
  }
}
