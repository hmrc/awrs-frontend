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
import forms.submapping.FieldNameUtil
import forms.test.util._
import forms.validation.util.ConstraintUtil.FormData
import forms.validation.util.FieldError
import models.TupleDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsFieldConfig

class BusinessDetailsFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  lazy val forms = (entity: String, hasAwrs: Boolean) => BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).form
  val SoleTrader = "SOP"
  val Ltd = "LTD"
  val Partnership = "Partnership"
  val LimitedLiabilityGroup = "LLP_GRP"
  val LimitedGroup = "LTD_GRP"

  val entities = Seq[String](SoleTrader, Ltd, Partnership, LimitedLiabilityGroup, LimitedGroup)

  "Business details form" should {
    for (entity <- entities) {
      implicit lazy val form = forms(entity, true)

      /*entity match {
        case LimitedLiabilityGroup | LimitedGroup => {
          f"check validations for businessName for entity: $entity" in {
            val fieldId = "companyName"

            val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.businessName_empty2"))
            val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "business name", AwrsFieldConfig.companyNameLen)
            val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "business name"))
            val formatError = ExpectedFieldFormat(invalidFormats)

            val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)
            fieldId assertFieldIsCompulsory expectations
          }
        }
        case _ =>
      }*/

      f"check validations for doYouHaveTradingName for entity: $entity" in {
        val fieldId = "doYouHaveTradingName"

        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_trading_name_empty"))

        val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
        fieldId assertEnumFieldIsCompulsory expectations
      }

      f"check validations for tradingName for entity: $entity" in {
        val fieldId = "tradingName"

        val preCondition: Map[String, String] = Map("doYouHaveTradingName" -> BooleanRadioEnum.Yes.toString)

        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_details.error.tradingName_empty"))
        val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "trading name", AwrsFieldConfig.tradingNameLen)
        val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "trading name"))
        val formatError = ExpectedFieldFormat(invalidFormats)

        val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)
        fieldId assertFieldIsCompulsoryWhen (preCondition, expectations)
      }

      f"check validations for newAWBusiness for entity: $entity" in {
        // N.B. this test is for the optional validation function used for proposed date is too early
        // the rest of the tests are covered by NewAWBuesinessMappingTest
        val prefix = "newAWBusiness"

        val dateId = prefix attach "proposedStartDate"
        val newBusinessAnsweredYes: FormData = FormData("newAWBusiness.newAWBusiness" -> "Yes")
        val expectations = CompulsoryDateValidationExpectations(
          ExpectedFieldIsEmpty(dateId, FieldError("awrs.business_details.error.proposedDate_empty")),
          ExpectedDateFormat(
            List(ExpectedInvalidDateFormat(
              TupleDate("31", "03", "2016"),
              dateId,
              FieldError("awrs.business_details.error.proposedDate_toEarly"))),
            List(
              // tests the too early range check function
              ExpectedValidDateFormat(TupleDate("01", "04", "2016"))
            )
          )
        )
        dateId assertDateFieldIsCompulsoryWhen(newBusinessAnsweredYes, expectations)
      }
    }
  }
}
