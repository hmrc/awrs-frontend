/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.prevalidation._
import models.AwrsEnrolmentUtr
import play.api.data.Form
import play.api.data.Forms._

object AwrsEnrolmentUtrForm {

  val utr = "utr"
  val (zero, one, two, three, four, five, six, seven, eight, nine, ten, eleven) = (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

  private def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
    val mapOfRemainders = Map(
      zero -> two, one -> one, two -> nine, three -> eight, four -> seven, five -> six,
      six -> five, seven -> four, eight -> three, nine -> two, ten -> one)
    mapOfRemainders.get(remainder).contains(checkDigit)
  }

  def validateUTR(utr: String): Boolean = {
    (utr.trim.length == 10 || utr.trim.length == 13) && utr.trim.forall(_.isDigit) && {
      val actualUtr = utr.trim.takeRight(10).toList
      val checkDigit = actualUtr.head.asDigit
      val restOfUtr = actualUtr.tail
      val weights = List(six, seven, eight, nine, ten, five, four, three, two)
      val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield w1 * u1.asDigit
      val total = weightedUtr.sum
      val remainder = total % eleven
      isValidUtr(remainder, checkDigit)
    }
  }

  lazy val awrsEnrolmentUtrValidationForm: Form[AwrsEnrolmentUtr] = Form(mapping(
    utr ->  text
      .verifying("awrs.utr.empty", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.length > 0
      })
      .verifying("awrs.utr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && (trimmedString.matches("""^[0-9]{10}$""") || trimmedString.matches("""^[0-9]{13}$""")))})
      .verifying("awrs.utr.invalidUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || !(trimmedString.matches("""^[0-9]{10}$""") || trimmedString.matches("""^[0-9]{13}$""")) || validateUTR(trimmedString)
      })
  )(AwrsEnrolmentUtr.apply)(AwrsEnrolmentUtr.unapply))

  lazy val awrsEnrolmentUtrForm: PrevalidationAPI[AwrsEnrolmentUtr] = PreprocessedForm(awrsEnrolmentUtrValidationForm)
}
