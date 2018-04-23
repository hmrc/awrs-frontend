/*
 * Copyright 2018 HM Revenue & Customs
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

package forms.test.util

import forms.validation.util.{FieldError, MessageArguments, MessageLookup, SummaryError}
import models.TupleDate
import play.api.data.Form


trait ImplicitSingleFieldTestAPI {

  implicit val fieldId: String

  def assertFieldIsCompulsory(config: CompulsoryFieldValidationExpectations): Unit

  def assertFieldIsCompulsoryWhen(condition: Map[String, String], config: CompulsoryFieldValidationExpectations): Unit

  def assertFieldIsCompulsoryWhen(conditions: Set[Map[String, String]], config: CompulsoryFieldValidationExpectations): Unit

  def assertFieldIsOptional(config: OptionalFieldValidationExpectations): Unit

  def assertFieldIsOptionalWhen(condition: Map[String, String], config: OptionalFieldValidationExpectations): Unit

  def assertFieldIsOptionalWhen(conditions: Set[Map[String, String]], config: OptionalFieldValidationExpectations): Unit

  def assertEnumFieldIsCompulsory(config: CompulsoryEnumValidationExpectations): Unit

  def assertEnumFieldIsCompulsoryWhen(condition: Map[String, String], config: CompulsoryEnumValidationExpectations): Unit

  def assertEnumFieldIsCompulsoryWhen(conditions: Set[Map[String, String]], config: CompulsoryEnumValidationExpectations): Unit

  def assertFieldIsIgnoredWhen(condition: Map[String, String], config: FieldToIgnore): Unit

  def assertFieldIsIgnoredWhen(conditions: Set[Map[String, String]], config: FieldToIgnore): Unit

  def assertEnumFieldIsIgnoredWhen(condition: Map[String, String], config: EnumFieldToIgnore): Unit

  def assertEnumFieldIsIgnoredWhen(conditions: Set[Map[String, String]], config: EnumFieldToIgnore): Unit

  def assertDateFieldIsCompulsory(config: CompulsoryDateValidationExpectations): Unit

  def assertDateFieldIsCompulsoryWhen(condition: Map[String, String], config: CompulsoryDateValidationExpectations): Unit

  def assertDateFieldIsCompulsoryWhen(conditions: Set[Map[String, String]], config: CompulsoryDateValidationExpectations): Unit

  def assertDateFieldIsIgnoredWhen(condition: Map[String, String], config: DateToIgnore): Unit

  def assertDateFieldIsIgnoredWhen(conditions: Set[Map[String, String]], config: DateToIgnore): Unit
}

trait ImplicitCrossFieldTestAPI {

  implicit val fieldIds: Set[String]

  def assertAtLeastOneFieldMustNotBeEmptyWhen(condition: Map[String, String], config: CrossFieldValidationExpectations): Unit

  def assertAtLeastOneFieldMustNotBeEmptyWhen(conditions: Set[Map[String, String]], config: CrossFieldValidationExpectations): Unit

  def assertAllFieldsCannotBeAnsweredWithInvalidWhen(condition: Map[String, String], config: CrossFieldValidationExpectations, invalidAnswer: String): Unit

  def assertAllFieldsCannotBeAnsweredWithInvalidWhen(conditions: Set[Map[String, String]], config: CrossFieldValidationExpectations, invalidAnswer: String): Unit

  def assertAtLeastOneFieldMustNotBeEmptyIsIgnoredWhen(condition: Map[String, String], config: CrossFieldValidationExpectations): Unit

  def assertAtLeastOneFieldMustNotBeEmptyIsIgnoredWhen(conditions: Set[Map[String, String]], config: CrossFieldValidationExpectations): Unit

  def assertAllFieldsCannotBeAnsweredWithInvalidIsIgnoredWhen(condition: Map[String, String], config: CrossFieldValidationExpectations, invalidAnswer: String): Unit

  def assertAllFieldsCannotBeAnsweredWithInvalidIsIgnoredWhen(conditions: Set[Map[String, String]], config: CrossFieldValidationExpectations, invalidAnswer: String): Unit
}

trait FormValidationTestAPI {

  def assertErrorMessageIsCorrectlyPopulated(errorMessage: MessageLookup): Unit

  def assertFormIsValid[T](form: Form[T], testData: Map[String, String]): Unit

  def assertFieldError(formWithErrors: Form[_], fieldId: String, expected: FieldError): Unit

  def assertDateFieldCannotBeEmpty(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, fieldIsEmptyExpectation: ExpectedFieldIsEmpty): Unit

  def assertNotThisFieldError(form: Form[_], fieldId: String, unacceptable: FieldError): Unit

  def assertHasNoFieldError(form: Form[_], fieldId: String): Unit

  def assertHasFieldError(form: Form[_], fieldId: String): Unit

  def assertSummaryError(formWithErrors: Form[_], fieldId: String, expected: SummaryError): Unit

  def assertNotThisSummaryError(form: Form[_], fieldId: String, unacceptable: SummaryError): Unit

  def assertHasNoAnchorFromSummaryError(form: Form[_], fieldId: String): Unit

  def assertFieldCannotBeEmpty(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, fieldIsEmptyExpectation: ExpectedFieldIsEmpty): Unit

  def assertFieldCannotBeExceedMaxLength(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, maxLengthExpectationOp: MaxLengthOption[ExpectedFieldExceedsMaxLength]): Unit

  def assertFieldConformsExpectedFormats(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, formatExpectations: ExpectedFieldFormat): Unit

  def assertDateFieldConformsExpectedFormats(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, formatConfig: ExpectedDateFormat): Unit

  def assertEnumFieldSatisfy(preCond: Map[String, String] = Map())(form: Form[_], fieldId: String, validEnumValues: Set[Enumeration#Value], invalidEnumValues: Set[Enumeration#Value]): Unit

  def assertFieldIgnoresEmptyConstraintWhen(preCond: Map[String, String])(form: Form[_], fieldId: String): Unit

  def assertFieldIgnoresMaxLengthConstraintWhen(preCond: Map[String, String])(form: Form[_], fieldId: String, maxLength: Int): Unit

  def assertFieldIgnoresFormatsConstraitsWhen(preCond: Map[String, String])(form: Form[_], fieldId: String, formatExpectations: ExpectedFieldFormat): Unit

  def assertFieldIgnoresDateFormatsConstraitsWhen(preCond: Map[String, String])(form: Form[_], fieldId: String, formatExpectations: ExpectedDateFormat): Unit

  def assertEnumFieldIgnoresConstraintsWhen(preCond: Map[String, String])(form: Form[_], fieldId: String, validEnumValues: Set[Enumeration#Value], invalidEnumValues: Set[Enumeration#Value]): Unit
}

trait ExpectedErrorExpectation {
  def fieldError: FieldError

  def summaryError: SummaryError
}

case class ExpectedFieldIsEmpty(val fieldError: FieldError, val summaryError: SummaryError) extends ExpectedErrorExpectation

object ExpectedFieldIsEmpty {
  def apply(anchorId: String, fieldError: FieldError): ExpectedFieldIsEmpty =
    new ExpectedFieldIsEmpty(fieldError, SummaryError(fieldError, anchorId))
}

case class ExpectedFieldExceedsMaxLength(val fieldError: FieldError, val summaryError: SummaryError, val maxLength: Int) extends ExpectedErrorExpectation

object ExpectedFieldExceedsMaxLength {
  // quick constructor for the default expected max length error messages
  def apply(fieldId: String, embeddedFieldNameInErrorMessages: String, maxLen: Int,defaultKey: String = "awrs.generic.error.maximum_length" ): ExpectedFieldExceedsMaxLength = {
    val defaultError = FieldError(defaultKey, MessageArguments(embeddedFieldNameInErrorMessages, maxLen))
    new ExpectedFieldExceedsMaxLength(defaultError, SummaryError(defaultError, MessageArguments(embeddedFieldNameInErrorMessages), fieldId), maxLen)
  }
}

sealed trait MaxLengthOption[+A] {
  def toOption = this match {
    case MaxLengthDefinition(maxLength) => Some(maxLength)
    case MaxLengthIsHandledByTheRegEx() => None
  }

  def nonEmpty: Boolean = this match {
    case MaxLengthDefinition(_) => true
    case MaxLengthIsHandledByTheRegEx() => false
  }

  def isEmpty: Boolean = this match {
    case MaxLengthDefinition(_) => false
    case MaxLengthIsHandledByTheRegEx() => true
  }
}

case class MaxLengthDefinition[A <: ExpectedFieldExceedsMaxLength](val get: A) extends MaxLengthOption[A]

case class MaxLengthIsHandledByTheRegEx() extends MaxLengthOption[Nothing]

case class ExpectedInvalidFieldFormat(val invalidCase: String, val fieldError: FieldError, val summaryError: SummaryError) extends ExpectedErrorExpectation

object ExpectedInvalidFieldFormat {
  def apply(invalidCase: String, fieldId: String, embeddedFieldNameInErrorMessages: String): ExpectedInvalidFieldFormat = {
    val defaultKey = "awrs.generic.error.character_invalid"
    val defaultFieldError = FieldError(defaultKey)
    val defaultSummaryError = SummaryError(defaultFieldError, MessageArguments(embeddedFieldNameInErrorMessages), fieldId)
    new ExpectedInvalidFieldFormat(invalidCase, defaultFieldError, defaultSummaryError)
  }

  def apply(invalidCase: String, fieldId: String, fieldError: FieldError): ExpectedInvalidFieldFormat = {
    val defaultSummaryError = SummaryError(fieldError, fieldId)
    new ExpectedInvalidFieldFormat(invalidCase, fieldError, defaultSummaryError)
  }
}

case class ExpectedValidFieldFormat(val validCase: String)

case class ExpectedFieldFormat(val invalidFormats: List[ExpectedInvalidFieldFormat], val validFormats: List[ExpectedValidFieldFormat] = List[ExpectedValidFieldFormat]())

case class CompulsoryFieldValidationExpectations(val fieldIsEmptyExpectation: ExpectedFieldIsEmpty, val maxLengthExpectation: MaxLengthOption[ExpectedFieldExceedsMaxLength], val formatExpectations: ExpectedFieldFormat) {
  def toOptionalFieldValidationExpectations: OptionalFieldValidationExpectations = new OptionalFieldValidationExpectations(maxLengthExpectation, formatExpectations)

  def toFieldToIgnore: FieldToIgnore = new FieldToIgnore(
    maxLengthExpectation match {
      case MaxLengthDefinition(maxLength) => Option(maxLength.maxLength)
      case _ => None
    }, formatExpectations)
}

object CompulsoryFieldValidationExpectations {
  def apply(fieldIsEmptyExpectation: ExpectedFieldIsEmpty, maxLengthExpectation: ExpectedFieldExceedsMaxLength, formatExpectations: ExpectedFieldFormat) =
    new CompulsoryFieldValidationExpectations(fieldIsEmptyExpectation, MaxLengthDefinition(maxLengthExpectation), formatExpectations)
}

case class OptionalFieldValidationExpectations(val maxLengthExpectation: MaxLengthOption[ExpectedFieldExceedsMaxLength], val formatExpectations: ExpectedFieldFormat) {
  def toFieldToIgnore: FieldToIgnore = new FieldToIgnore(
    maxLengthExpectation match {
      case MaxLengthDefinition(maxLength) => Option(maxLength.maxLength)
      case _ => None
    }, formatExpectations)
}

object OptionalFieldValidationExpectations {
  def apply(maxLengthExpectation: ExpectedFieldExceedsMaxLength, formatExpectations: ExpectedFieldFormat) =
    new OptionalFieldValidationExpectations(MaxLengthDefinition(maxLengthExpectation), formatExpectations)
}

case class CompulsoryEnumValidationExpectations(val fieldIsEmptyExpectation: ExpectedFieldIsEmpty, val validEnumValues: Set[Enumeration#Value], val invalidEnumValues: Set[Enumeration#Value]) {
  def toIgnoreEnumFieldExpectation: EnumFieldToIgnore = new EnumFieldToIgnore(fieldIsEmptyExpectation, validEnumValues, invalidEnumValues)
}

object CompulsoryEnumValidationExpectations {
  private val empty: Set[Enumeration#Value] = Set[Enumeration#Value]()

  def apply(fieldIsEmptyExpectation: ExpectedFieldIsEmpty, expectedEnum: Enumeration) = new CompulsoryEnumValidationExpectations(fieldIsEmptyExpectation, expectedEnum.values.toSet, empty)

  //TODO add constructor to auto add unused enum#values from the enum to the ignore list
}

case class ExpectedValidDateFormat(val validCase: TupleDate)

case class ExpectedInvalidDateFormat(val invalidCase: TupleDate, val fieldError: FieldError, val summaryError: SummaryError) extends ExpectedErrorExpectation

object ExpectedInvalidDateFormat {
  def apply(invalidCase: TupleDate, fieldId: String, fieldError: FieldError): ExpectedInvalidDateFormat = {
    val defaultSummaryError = SummaryError(fieldError, fieldId)
    new ExpectedInvalidDateFormat(invalidCase, fieldError, defaultSummaryError)
  }
}

case class ExpectedDateFormat(val invalidFormats: List[ExpectedInvalidDateFormat], val validFormats: List[ExpectedValidDateFormat] = List[ExpectedValidDateFormat]())

case class CompulsoryDateValidationExpectations(val fieldIsEmptyExpectation: ExpectedFieldIsEmpty, val formatExpectations: ExpectedDateFormat) {
  def toDateToIgnore: DateToIgnore = new DateToIgnore(None, formatExpectations)
}
case class DateToIgnore(val maxLength: Option[Int], val formatExpectations: ExpectedDateFormat)

case class FieldToIgnore(val maxLength: Option[Int], val formatExpectations: ExpectedFieldFormat)

case class EnumFieldToIgnore(val fieldIsEmptyExpectation: ExpectedFieldIsEmpty, val validEnumValues: Set[Enumeration#Value], val invalidEnumValues: Set[Enumeration#Value])

object EnumFieldToIgnore {
  private val empty: Set[Enumeration#Value] = Set[Enumeration#Value]()

  def apply(fieldIsEmptyExpectation: ExpectedFieldIsEmpty, expectedEnum: Enumeration) = new EnumFieldToIgnore(fieldIsEmptyExpectation, expectedEnum.values.toSet, empty)
}

case class CrossFieldValidationExpectations(val anchor: String, val fieldIsEmptyExpectation: ExpectedFieldIsEmpty)


/**
  * A class used to specify prefix for ids
  *
  * It's intended to be used by FieldNameUtilAPI and ImplicitFieldNameUtil
  * to easily attach prefix to ids when it is abscent or supplied
  *
  * @param prefix
  */
case class IdPrefix(val prefix: Option[String])

/**
  * implicit conversions from String and Option[String] to IdPrefix
  */
object IdPrefix {
  def apply(str: String): IdPrefix = new IdPrefix(Some(str))

  implicit def fromString(str: String): IdPrefix = new IdPrefix(Some(str))

  implicit def fromString(str: Option[String]): IdPrefix = new IdPrefix(str)
}

/**
  * function designed to be used by ImplicitFieldNameUtil
  * to allow easy attachment of prefix to field ids regardless of whether the
  * prefix is supplied
  */
trait FieldNameUtilAPI {
  def attach(fieldId: String): String

  def attachToAll(fieldIds: Set[String]): Set[String]
}
