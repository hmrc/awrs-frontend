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

package utils

import play.api.data.validation.{Constraint, Invalid, Valid}

object AwrsValidator extends AwrsValidator

trait AwrsValidator {

  val ninoRegex = """^(?i)[ \t]*[A-Z]{1}[ \t]*[ \t]*[A-Z]{1}[ \t]*[0-9]{1}[ \t]*[ \t]*[0-9]{1}[ \t]*""" +
    """[ \t]*[0-9]{1}[ \t]*[ \t]*[0-9]{1}[ \t]*[ \t]*[0-9]{1}[ \t]*[ \t]*[0-9]{1}[ \t]*[A-D]{1}[ \t]*$"""

  // match leading spaces + 'GB' + any combination of digits and spaces 8 times, case insensitive
  val vatRegex = """^(?i)([ \t]*G[ \t]*B[ \t]*(?:[ \t]*\d[ \t]*){9})|(?:[ \t]*\d[ \t]*){9}$"""

  // match any combination of alphanumeric characters and spaces 8 times, case insensitive
  val crnRegex =  """^(?i)(?:[ \t]*[A-Z0-9][ \t]*){8}$"""

  // match leadings spaces + any combination of digits and spaces 10 times
  val utrRegex = """^(?:[ \t]*\d[ \t]*){10}$"""

  val emailRegex = """(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$)"""

  val postcodeRegex = """(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"""

  val dateRegex =  """^\d{2}/\d{2}/\d{4}$"""

  val asciiRegex =  """^[\x00-\x7F]*$"""

  val alphaRegex = """^(?i)[A-Z ]+$"""

  val numericRegex = """^[0-9 ]+$"""

  val alphaNumericRegex = """^(?i)[A-Z0-9 ]+$"""

  // allow any combination of numbers and letters from 1 to 20 times
  // for now both passport and national ids are the same but the variables are left seperate in case if they do diverge
  private val passportNatIdRegex = """^(?i)(?:[ \t]*[a-z0-9][ \t]*){1,20}$"""

  val passportRegex = passportNatIdRegex

  val nationalIDRegex = passportNatIdRegex

  val telephoneRegex = """^[0-9]*+"""

  val operatingDurationRegex = """^[0-9]*+"""

  def isValidNino(errorMsg: String): Constraint[String] = Constraint[String]("nino"){
    case nino if nino.matches(ninoRegex) => Valid
    case _ => Invalid(errorMsg)
  }

  val asciiChar32 = 32
  val asciiChar126 = 126
  val asciiChar160 = 160
  val asciiChar255 = 255

  def validText(input: String): Boolean = {
    validateISO88591(input)
  }

  def validateISO88591(input: String): Boolean = {
    val inputList: List[Char] = input.toList
    inputList.forall { c =>
      (c >= asciiChar32 && c <= asciiChar126) || (c >= asciiChar160 && c <= asciiChar255)
    }
  }

  def validTextRegex(regex: String): (String) => Boolean = (input: String) => input.matches(regex)

  val validAlphaNumeric = validTextRegex(alphaNumericRegex)

}
