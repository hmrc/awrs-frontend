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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package forms
//
//import forms.AWRSEnums.BooleanRadioEnum
//import forms.submapping.FieldNameUtil
//import forms.test.util._
//import forms.validation.util.ConstraintUtil.FormData
//import forms.validation.util.FieldError
//import models.{ExtendedBusinessDetails, TupleDate}
//import org.scalatestplus.mockito.MockitoSugar
//import org.scalatestplus.play.PlaySpec
//import play.api.data.Form
//import play.api.i18n.{Messages, MessagesApi}
//import play.api.test.FakeRequest
//import utils.AwrsFieldConfig
//import utils.TestConstants._
//
//class BusinessDetailsFormTest extends PlaySpec with MockitoSugar  with AwrsFieldConfig with AwrsFormTestUtils {
//  lazy val forms = (entity: String, hasAwrs: Boolean) => BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).form
//  val SoleTrader = "SOP"
//  val Ltd = "LTD"
//  val Partnership = "Partnership"
//  val LimitedLiabilityGroup = "LLP_GRP"
//  val LimitedGroup = "LTD_GRP"
//
//  val entities = Seq[String](SoleTrader, Ltd, Partnership, LimitedLiabilityGroup, LimitedGroup)
//  val welshCharEntities = Seq[String](LimitedLiabilityGroup, LimitedGroup)
//
//  "Business details form" must {
//    for (entity <- entities; hasAwrs <- Seq(true, false)) {
//      implicit lazy val form = forms(entity, hasAwrs)
//
//      (entity, hasAwrs) match {
//        case (LimitedLiabilityGroup | LimitedGroup, true) => {
//          f"check validations for businessName for entity: $entity and hasAwrs: $hasAwrs" when {
//            val fieldId = "companyName"
//            val fieldNameInErrorMessage = "business name"
//
//            "field is left empty" in {
//              BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "")).fold(
//                formWithErrors => {
//                  formWithErrors.errors(fieldId).size mustBe 1
//                  formWithErrors.errors(fieldId).head.message mustBe "awrs.generic.error.businessName_empty"
//                },
//                _ => fail("Form should throw error")
//              )
//            }
//
//            "the maxLength of the field is exceeded"  in {
//              BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "a" * 141)).fold(
//                formWithErrors => {
//                  formWithErrors.errors(fieldId).size mustBe 1
//                  messages(formWithErrors.errors(fieldId).head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, companyNameLen)
//                },
//                _ => fail("Form should throw error")
//              )
//            }
//
//            "the invalid characters of the field are invoked"  in {
//              BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "α")).fold(
//                formWithErrors => {
//                  formWithErrors.errors(fieldId).size mustBe 1
//                  messages(formWithErrors.errors(fieldId).head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
//                },
//                _ => fail("Form should throw error")
//              )
//            }
//          }
//
//          }
//
//        case (LimitedLiabilityGroup | LimitedGroup, false) => {
//          f"check validations for businessName for entity: $entity and hasAwrs: $hasAwrs" when {
//            val fieldId = "companyName"
//
//            "field is left empty" in {
//              BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "")).fold(
//                formWithErrors => {
//                  formWithErrors.errors(fieldId).size mustBe 0
//                },
//                _ => fail("Form should throw error")
//              )
//            }
//          }
//        }
//        case _ =>
//      }
//
//      f"check validations for doYouHaveTradingName for entity: $entity and hasAwrs: $hasAwrs" in {
//        val fieldId = "doYouHaveTradingName"
//        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_trading_name_empty"))
//
//        val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
//        fieldId assertEnumFieldIsCompulsory expectations
//      }
//
//      f"check validations for tradingName for entity: $entity and hasAwrs: $hasAwrs" when {
//        val fieldId = "tradingName"
//        val preCondition: Map[String, String] = Map("doYouHaveTradingName" -> BooleanRadioEnum.Yes.toString)
//        val fieldNameInErrorMessage = "trading name"
//
//        "field is left empty" in {
//          BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "") ++ preCondition).fold(
//            formWithErrors => {
//              formWithErrors.errors(fieldId).size mustBe 1
//              formWithErrors.errors(fieldId).head.message mustBe "awrs.generic.error.tradingName_empty"
//            },
//            _ => fail("Form should throw error")
//          )
//        }
//
//        "the maxLength of the field is exceeded"  in {
//          BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "a" * 121) ++ preCondition).fold(
//            formWithErrors => {
//              formWithErrors.errors(fieldId).size mustBe 1
//              messages(formWithErrors.errors(fieldId).head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, tradingNameLen)
//            },
//            _ => fail("Form should throw error")
//          )
//        }
//
//        "the invalid characters of the field are invoked"  in {
//          BusinessDetailsForm.businessDetailsForm(entity, hasAwrs).bind(Map(fieldId -> "α") ++ preCondition).fold(
//            formWithErrors => {
//              formWithErrors.errors(fieldId).size mustBe 1
//              messages(formWithErrors.errors(fieldId).head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
//            },
//            _ => fail("Form should throw error")
//          )
//        }
//      }
//
//      f"check validations for newAWBusiness for entity: $entity and hasAwrs: $hasAwrs" when {
//        // N.B. this test is for the optional validation function used for proposed date is too early
//        // the rest of the tests are covered by NewAWBusinessMappingTest
//        val prefix = "newAWBusiness"
//        val dateId = prefix attach "proposedStartDate"
//        val newBusinessAnsweredYes: FormData = FormData("newAWBusiness.newAWBusiness" -> "Yes")
//
//        "the date field is left empty" in {
//
//        }
//
//        val expectations = CompulsoryDateValidationExpectations(
//          ExpectedFieldIsEmpty(dateId, FieldError("awrs.business_details.error.proposedDate_empty")),
//          ExpectedDateFormat(
//            List(ExpectedInvalidDateFormat(
//              TupleDate("31", "03", "2016"),
//              dateId,
//              FieldError("awrs.business_details.error.proposedDate_tooEarly"))),
//            List(
//              // tests the too early range check function
//              ExpectedValidDateFormat(TupleDate("01", "04", "2016"))
//            )
//          )
//        )
//        dateId assertDateFieldIsCompulsoryWhen(newBusinessAnsweredYes, expectations)
//      }
//    }
//
//    for (entity <- welshCharEntities; hasAwrs <- Seq(true))
//    f"check Welsh character validations for entity: $entity and hasAwrs: $hasAwrs" in {
//      List(ExpectedValidDateFormat(TupleDate("01", "04", "2016")))
//      val data: Map[String, String] =
//        Map("companyName" -> testWelshChars,
//          "newAWBusiness.newAWBusiness" -> "No",
//          "doYouHaveTradingName" -> "Yes",
//          "tradingName" -> testWelshChars
//        )
//      assertFormIsValid(forms(entity, hasAwrs), data)
//    }
//  }
//}
