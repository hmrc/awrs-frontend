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

import play.api.data.{Field, Form, FormError}
import play.api.i18n.Messages


/**
  * This API is designed to be used by the views to extract the relevant error fields from "form with errors"
  * post validation. The format of the Invalid instances within the error fields are expected to be generated by
  * the ErrorMessageFactory API
  */
trait ErrorMessageInterpreter {

  def getFieldErrors(field: Field, parent: Field)(implicit messages: Messages): Seq[FieldError]

  def getFieldErrors(field: Field, form: Form[_])(implicit messages: Messages): Seq[FieldError]

  def getFieldErrors(field: Field, parent: Option[Field] = None)(implicit form: Option[Form[_]] = None, messages: Messages): Seq[FieldError]

  def getFieldErrors(field: String, form: Form[_])(implicit messages: Messages): Seq[FieldError]

  def getFieldErrorsByName(field: String)(implicit form: Option[Form[_]] = None, messages: Messages): Seq[FieldError]

  def getSummaryErrors(form: Form[_])(implicit messages: Messages): Seq[SummaryError]

  def defaultSummaryId(fieldId: String): String
}


// used to extract the relevant information required by the html views from validation form errors.
// these functions expect the form errors to wholly consist API supported Invalid instances
object ErrorMessageInterpreter extends ErrorMessageInterpreter {

  import ErrorDelimiterConstants._

  /**
    * *******************************************************************************************
    * start of the embedded message extraction madness, this will be replaced by the much
    * simple type matching version as soon as we no longer need the backwards compatibility
    * with the string manipulation code base
    * *******************************************************************************************
    */
  private def processEmbedded(str: String)(implicit messages: Messages): String =
  messages(FieldId(str), extractParam(str).args: _*)

  private def extract(str: String)(implicit messages: Messages): MessageArguments = {
    val close: Int = str.indexOf(embeddedEnd)
    if (close != -1) {
      val open: Int = str.substring(0, close).lastIndexOf(embeddedStart)
      val innerContent: String = str.substring(open + embeddedStart.length, close)
      val processed = str.substring(0, open) + processEmbedded(innerContent) + str.substring(close + embeddedEnd.length, str.length)
      extract(processed)
    } else {
      extractParam(str)
    }
  }

  private def extractParam(messageString: String)(implicit messages: Messages): MessageArguments =
    if (messageString.contains(embeddedStart) && messageString.contains(embeddedEnd)) {
      extract(messageString)
    } else {
      val msgParam: Array[String] = messageString.split(paramDelimiter, -1).drop(1)
      MessageArguments(msgParam: _*)
    }

  /** ************************************ end madness ********************************************/

  private def formatFieldError(error: FormError)(implicit messages: Messages): FieldError = {
    val msgkey: String = FieldId(error.message)
    val params: MessageArguments = extractParam(error.message.split(fieldDelimiter, -1).last)
    FieldError(msgkey, params)
  }

  // not sure what when this is used
  // it's left in to guarantee backwards compatibility
  def getFieldErrors(field: Field, parent: Field)(implicit messages: Messages): Seq[FieldError] = {
    parent.errors.foldLeft[Seq[FieldError]](field.errors.map(error => formatFieldError(error))) { (errors, error) =>
      error.args.map { arg =>
        parent.name + "." + arg
      }.contains(field.name) match {
        case true => formatFieldError(error) +: errors
        case _ => errors
      }
    }
  }

  private def paramContainsId(id: String, args: Seq[Any]): Boolean = args.map {
    case arg: String => arg == id //only used for backwards compatibility
    case TargetFieldIds(anchor, otherids@_*) if otherids.isEmpty => anchor.equals(id)
    case TargetFieldIds(anchor, otherids@_*) => anchor.equals(id) || otherids.contains(id)
  }.fold(false)(_ || _)

  // not sure what field.name == error.args.fold(error.key) { _ + "." + _ } is intended for
  // it's only left in to guarantee backwards compatibility
  def getFieldErrors(field: Field, form: Form[_])(implicit messages: Messages): Seq[FieldError] = {
    lazy val filtered =
      form.errors.filter { error =>
        error.key == field.name || paramContainsId(field.name, error.args) || field.name ==
          error.args.fold(error.key)(_ + "." + _)
      }
    filtered.map(error => formatFieldError(error))
  }

  def getFieldErrors(field: Field, parent: Option[Field] = None)(implicit form: Option[Form[_]] = None, messages: Messages): Seq[FieldError] = {
    parent match {
      case Some(parent) => getFieldErrors(field, parent)
      case _ => form match {
        case Some(form) => getFieldErrors(field, form)
        case _ => field.errors.map(error => formatFieldError(error))
      }
    }
  }

  def getFieldErrors(field: String, form: Form[_])(implicit messages: Messages): Seq[FieldError] = {
    lazy val filtered = form.errors.filter { error => error.key == field || error.args.contains(field) || field == error.args.fold(error.key) {
      _ + "." + _
    }
    }
    filtered.map(error => formatFieldError(error))
  }

  /*
   * This is a routing function to determine how to resolve the errors on a field depending on whether a parent field is passed or a form reference is in scope
   */
  def getFieldErrorsByName(field: String)(implicit form: Option[Form[_]] = None, messages: Messages): Seq[FieldError] = {
    form match {
      case Some(form) => getFieldErrors(field, form)
      case _ => Seq()
    }
  }


  private def getAnchorId(args: Seq[Any]): String = args.filter { arg =>
    arg match {
      case TargetFieldIds(_, _*) => true
      case _: String => true
      case _ => false
    }
  }.head match {
    case TargetFieldIds(anchor, _*) => anchor
    case a: String => a
    case _ => ""
  }

  def getSummaryErrors(form: Form[_])(implicit messages: Messages): Seq[SummaryError] =
    form.errors.map { error =>
      val anchor = error.args.nonEmpty match {
        case true => {
          error.args.head match {
            case arg: String =>
              if (error.key.nonEmpty) {
                error.key + error.args.fold("")(_ + "." + _)
              } else {
                getAnchorId(error.args)
              }
            case TargetFieldIds(anchor, _*) => getAnchorId(error.args)
          }

        }
        case _ => {
          error.key
        }
      }
      val msg: String = SummaryId(error.message)
      val params: MessageArguments = SummaryParam(error.message)
      SummaryError(msg, params, anchor.toString)
    }

  // current string manipulation error extraction functions
  import ErrorDelimiterConstants._

  def defaultSummaryId(fieldId: String): String = fieldId

  private def FieldId(errMsg: String): String =
    errMsg.split(fieldDelimiter, -1).last.split(paramDelimiter, -1).head

  private def SummaryId(errMsg: String): String = errMsg contains (summaryIdMarker) match {
    case true => errMsg.split(fieldDelimiter, -1).head.split(summaryIdMarker, -1).head
    case false => defaultSummaryId(FieldId(errMsg))
  }

  /**
    * *******************************************************************************************
    * start of the embedded message extraction madness, this will be replaced by the much
    * simple type matching version as soon as we no longer need the backwards compatibility
    * with the string manipulation code base
    * *******************************************************************************************
    */

  private def keyId(str: String) = str.split(paramDelimiter, -1).head

  private def processSummaryEmbedded(str: String)(implicit messages: Messages): String = {
    val key = keyId(str)
    val args = str.substring(str.indexOf(paramDelimiter) + paramDelimiter.length, str.length)
    messages(key, extractSummaryParam(args).args: _*)
  }

  private def extract2(str: String)(implicit messages: Messages): MessageArguments = {
    val close: Int = str.indexOf(embeddedEnd)
    if (close != -1) {
      val open: Int = str.substring(0, close).lastIndexOf(embeddedStart)
      val innerContent: String = str.substring(open + embeddedStart.length, close)
      val processed = str.substring(0, open) + processSummaryEmbedded(innerContent) + str.substring(close + embeddedEnd.length, str.length)
      extract2(processed)
    } else {
      extractSummaryParam(str)
    }
  }

  private def extractSummaryParam(messageString: String)(implicit messages: Messages): MessageArguments =
    if (messageString.contains(embeddedStart) && messageString.contains(embeddedEnd)) {
      extract2(messageString)
    } else {
      val msgParam: Array[String] = messageString.split(paramDelimiter, -1)
      MessageArguments(msgParam: _*)
    }

  /** ************************************ end madness ********************************************/

  private def SummaryParam(errMsg: String)(implicit messages: Messages): MessageArguments =
  errMsg contains summaryIdMarker match {
    case true => errMsg.split(fieldDelimiter, -1).head.split(summaryIdMarker, -1).drop(1) match {
      case x if x.isEmpty => MessageArguments()
      case x => extractSummaryParam(x.last)
    }
    case false => errMsg match {
      case x if !x.contains(fieldDelimiter) => MessageArguments()
      case x => extractSummaryParam(x.split(fieldDelimiter, -1).head)
    }
  }

}
