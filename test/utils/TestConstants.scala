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

package utils

import scala.util.Random
import org.joda.time.LocalDate
import uk.gov.hmrc.domain.{Generator, SaUtr, SaUtrGenerator}

import scala.annotation.tailrec

object TestConstants {

  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testUtr: String = new SaUtrGenerator().nextSaUtr.utr
  val prefixedLowerCaseUTR: SaUtr = SaUtr("abc123451235")
  val prefixedCapsUTR: SaUtr = SaUtr("ABC123451235")
  lazy val testVrn: String = genRandNumString(9)
  lazy val testCrn: String = s"1${genRandNumString(7)}"
  val shortCRN = "2134567"
  val alphaNumCRN = "AB123456"
  lazy val testNonMatchingUtr: String = new SaUtrGenerator().nextSaUtr.utr
  lazy val testAWRSUtr: String = "XNAW" + genRandNumString(11)
  lazy val testCTUtr: String = genRandNumString(12)
  lazy val testOrg = "testOrg"
  //  lazy val testNino = new Generator().nextNino.nino
  //  lazy val testUtr = new SaUtrGenerator().nextSaUtr.utr
  lazy val testPassportNo: String = "1" * 20
  lazy val testNationalId: String = "1" * 20
  lazy val testGrpJoinDate: String = LocalDate.now().toString()
  lazy val testRefNo = "DummmyRef"
  lazy val testPostcode: String = genPostCode
  lazy val testEmail = "email@email.com"
  lazy val testTradingName = "North East Wines"
  lazy val testUserName = "joe bloggs"
  lazy val testWelshChars = "ôéàëŵŷáîïâêûü"

  // a hacky method to generate a post code by testing a probable post code against the regex
  @tailrec
  def genPostCode: String = {
    // function used to create a random string using only values within a specified char set
    def randomString(length: Int, chars: Seq[Char]): String = {
      val tmpList = List.range(0, length)
      val charList = tmpList.map { e => chars(Random.nextInt(chars.length)) }
      charList.mkString
    }

    // this generates: ^[A-Z]{2}[0-9][0-9]? [0-9][A-Z]{2}$
    val probablePostCode: String = randomString(2, 'A' to 'Z') + Random.nextInt(99).toString + ' ' + Random.nextInt(9).toString + randomString(2, 'A' to 'Z')

    probablePostCode.matches(AwrsValidator.postcodeRegex) match {
      case true => probablePostCode
      case false => genPostCode
    }
  }

  def genRandNumString(length: Int): String = Random.nextInt(9).toString * length

}
