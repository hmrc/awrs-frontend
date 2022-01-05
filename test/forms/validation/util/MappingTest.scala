/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.validation.util

import forms.test.util._
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, FieldFormatConstraintParameter, _}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI.{MappingUtil, _}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import play.api.data.{FieldMapping, Form, FormError}
import utils.AwrsValidator._

class MappingTest extends AwrsFormTestUtils with AnyWordSpecLike with MockitoSugar with GuiceOneAppPerSuite {

  implicit class Helper(errors: Seq[FormError]) {
    def shouldContain(expected: Invalid): Boolean = {
      val transformed: Seq[(String, Seq[Any])] = errors.map { x => (x.message, x.args) }
      transformed.contains((expected.errors.head.message, expected.errors.head.args))
    }
  }

  // this test evaluates the functionalities of the unbinding methods
  // this test expects valid form data as input
  // it fills the form then tests the fill method of the form.
  // the correctness of the fill method depends on the unbinding method
  def performFillFormTest[T](form: Form[T], data: FormData) {
    val model = form.bind(data).get
    val form2 = form.fill(model).get
    form2 mustBe model
  }

  val defaultMaxLength = 10

  val defaultCompulsoryMapping: String => FieldMapping[Option[String]] = (fieldId: String) => compulsoryText(CompulsoryTextFieldMappingParameter(
    simpleFieldIsEmptyConstraintParameter(fieldId, "error.empty"),
    FieldMaxLengthConstraintParameter(defaultMaxLength, Invalid("awrs.generic.error.maximum_length", fieldId, defaultMaxLength)),
    FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid", ""))

  ))

  val defaultOptionalMapping: String => FieldMapping[Option[String]] = (fieldId: String) => optionalText(OptionalTextFieldMappingParameter(
    FieldMaxLengthConstraintParameter(defaultMaxLength, Invalid("awrs.generic.error.maximum_length", fieldId, defaultMaxLength)),
    FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid", ""))
  ))

  "For single layer models" must {
    "mapping validations" must {
      case class Test(field1: Option[String],
                      field1a: String,
                      field2: Option[String],
                      ignoredField: Option[String],
                      ignoredField2: Option[String]
                     )

      val alwaysIgnore: Option[FormQuery] = (_: FormData) => false

      implicit val form = Form(
        mapping(
          "field1" -> defaultCompulsoryMapping("field1"),
          "field1a" -> defaultCompulsoryMapping("field1a").toStringFormatter,
          "field2" -> defaultOptionalMapping("field2"),
          "ignoredField" -> (defaultCompulsoryMapping("ignoredField") iff alwaysIgnore),
          "ignoredField2" -> (defaultOptionalMapping("ignoredField2") iff alwaysIgnore)
        )(Test.apply)(Test.unapply))

      "trigger validation errors" in {
        compulsoryFieldTest("field1", defaultMaxLength)
        compulsoryFieldTest("field1a", defaultMaxLength)
        optionalFieldTest("field2", defaultMaxLength)
      }

      "accept valid data, and can be populated by valid case class" in {
        val data = FormData("field1" -> "me", "field1a" -> "me")
        performFillFormTest(form, data)
      }

      "ignored fields must not be entered into the resulting case class" in {
        val data = FormData("field1" -> "me", "field1a" -> "me2", "ignoredField" -> "data", "ignoredField" -> "data2")
        form.bind(data).fold(
          _ => {},
          model => {
            model.ignoredField mustBe None
            model.ignoredField2 mustBe None
            model.field1 mustBe Some("me")
            model.field1a mustBe "me2"
          }
        )
      }
    }

    "function correctly when there is crossField validation" must {

      def field1IsEmpty(): Option[FormQuery] =
        (data: FormData) => data.getOrElse("field1", "").equals("")

      val field1IsEmptyTestData = FormData("field1" -> "")
      val field1IsNotEmptyTestData = FormData("field1" -> "a")

      // if a field is conditionally dependent on another then it cannot be of a String type
      case class Test(field1: Option[String],
                      field2: Option[String],
                      field3: Option[String]
                     )
      implicit val form = Form(
        mapping(
          "field1" -> optional(text),
          "field2" -> (defaultCompulsoryMapping("field2") iff field1IsEmpty),
          "field3" -> (defaultOptionalMapping("field3") iff field1IsEmpty)
        )(Test.apply)(Test.unapply))

      "trigger validation errors" in {
        compulsoryFieldTest("field2",defaultMaxLength, field1IsEmptyTestData)
        optionalFieldTest("field3", defaultMaxLength, field1IsEmptyTestData)
      }

      "not trigger validation errors when the fields are ignored" in {
        ignoredFieldTest("field2", field1IsNotEmptyTestData)
        ignoredFieldTest("field3", field1IsNotEmptyTestData)
      }

      "accept valid data, and can be populated by valid case class" in {
        val data = FormData(
          "field1" -> "",
          "field2" -> "me",
          "field3" -> "me")
        performFillFormTest(form, data)
      }
    }

    "For multi-layer models" must {
      "function correctly when it is used inside a submap" must {

        def field1IsEmpty(): Option[FormQuery] =
          (data: FormData) => data.getOrElse("sub.field1", "").equals("")

        val field1IsEmptyTestData = FormData("sub.field1" -> "")
        val field1IsNotEmptyTestData = FormData("sub.field1" -> "a")

        case class SubTest(field1: Option[String], field2: Option[String])
        case class Test(field1: SubTest)

        def attachPrefix(prefix: Option[String], fieldId: String): String = {
          prefix match {
            case None => fieldId
            case Some(p) => f"$p.$fieldId"
          }
        }

        // for sub mappings the invalid messages could change depending on the prefix
        // therefore all submappings must be configurable functions to allow customisation
        val submap = (prefix: Option[String]) => mapping(
          "field1" -> optional(text),
          "field2" -> (defaultCompulsoryMapping(attachPrefix(prefix, "field2")) iff field1IsEmpty)
        )(SubTest.apply)(SubTest.unapply)

        implicit val form = Form(mapping(
          "sub" -> submap(Some("sub"))
        )(Test.apply)(Test.unapply))

        "trigger validation errors" in {
          compulsoryFieldTest("sub.field2", defaultMaxLength, field1IsEmptyTestData)
        }

        "not trigger validation errors when the field is ignored" in {
          ignoredFieldTest("sub.field2", field1IsNotEmptyTestData)
        }

        "accept valid data, and can be populated by valid case class" in {
          val data = FormData("sub.field1" -> "", "sub.field2" -> "me")
          performFillFormTest(form, data)
        }
      }
    }
  }
}
