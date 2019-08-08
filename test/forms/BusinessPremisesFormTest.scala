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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class BusinessPremisesFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form = BusinessPremisesForm.businessPremisesForm.form

  "Form validation" should {
    "display the correct validation errors for additionalPremises" in {
      val fieldId = "additionalPremises"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.additional-premises.error.do_you_have_additional_premises"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      fieldId assertEnumFieldIsCompulsory expectations
    }

    "display correct validation for additionalAddress" in {
      val preCondition: Map[String, String] = Map("additionalPremises" -> BooleanRadioEnum.Yes.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("additionalPremises" -> BooleanRadioEnum.No.toString))
      val idPrefix: String = "additionalAddress"

      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition, idPrefix)
    }

    "display correct validation for addAnother" in {
      val fieldId = "addAnother"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.additional-premises.error.add_another"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      val preCondition: Map[String, String] = Map("additionalPremises" -> BooleanRadioEnum.Yes.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("additionalPremises" -> BooleanRadioEnum.No.toString))

      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
    }
  }

  "Form validation" should {
    "Allow submission if additionalPremises is answered with 'No'" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.No.toString)
      assertFormIsValid(form, data)
    }
    "Allow submission if additionalPremises is answered with 'Yes" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> "addressLine1",
          "additionalAddress.addressLine2" -> "addressLine2",
          "additionalAddress.addressLine3" -> "addressLine3",
          "additionalAddress.addressLine4" -> "addressLine4",
          "addAnother" -> BooleanRadioEnum.Yes.toString
        )
      assertFormIsValid(form, data)

      val data2: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> "addressLine1",
          "additionalAddress.addressLine2" -> "addressLine2",
          "addAnother" -> BooleanRadioEnum.No.toString
        )
      assertFormIsValid(form, data2)
    }
    "Allow submission if Welsh characters are input" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> testWelshChars,
          "additionalAddress.addressLine2" -> testWelshChars,
          "additionalAddress.addressLine3" -> testWelshChars,
          "additionalAddress.addressLine4" -> testWelshChars,
          "addAnother" -> BooleanRadioEnum.Yes.toString
        )
      assertFormIsValid(form, data)
    }
  }
}
