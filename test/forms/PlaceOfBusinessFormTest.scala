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
import forms.AWRSEnums.{BooleanRadioEnum, OperatingDurationEnum}
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class PlaceOfBusinessFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val forms = PlaceOfBusinessForm.placeOfBusinessForm.form

  "Business contacts form" should {

    f"check validations for mainAddress " in {
      val preCondition: Map[String, String] = Map("mainPlaceOfBusiness" -> BooleanRadioEnum.No.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("mainPlaceOfBusiness" -> BooleanRadioEnum.Yes.toString))
      val idPrefix: String = "mainAddress"

      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition, idPrefix, nameInErrorMessage = "principal place of business")
    }

    f"check validations for placeOfBusinessAddressLast3Years " in {
      val preCondition: Map[String, String] = Map("placeOfBusinessLast3Years" -> BooleanRadioEnum.No.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("placeOfBusinessLast3Years" -> BooleanRadioEnum.Yes.toString))
      val idPrefix: String = "placeOfBusinessAddressLast3Years"

      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition, idPrefix, nameInErrorMessage = "previous principal place of business")
    }

    "check the operating duration is selected" in {
      val fieldId = "operatingDuration"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_contacts.error.operating_duration_empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, OperatingDurationEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }

  }

}
