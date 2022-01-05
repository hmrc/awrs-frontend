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

package views.html

import play.api.mvc.{AnyContent, Request}
import utils.SessionUtil
import utils.AwrsFieldConfig

package object helpers extends AwrsFieldConfig {

  implicit def argConv[T](arg: T): Option[T] = Some(arg)

  implicit val sessionUtil: Request[AnyContent] => SessionUtil.SessionUtilForRequest = SessionUtil.sessionUtilForRequest

  val frontendDefaultLen: String = "40"

  val ordinalIntSuffix: Int => String =
    {
      case 1 => "first"
      case 2 => "second"
      case 3 => "third"
      case 4 => "fourth"
      case 5 => "fifth"
      case 6 => "sixth"
      case 7 => "seventh"
      case 8 => "eighth"
      case 9 => "ninth"
      case 10 => "tenth"
      case 11 => "eleventh"
      case 12 => "twelfth"
      case 13 => "thirteenth"
      case 14 => "fourteenth"
      case 15 => "fifteenth"
      case 16 => "sixteenth"
      case 17 => "seventeenth"
      case 18 => "eighteenth"
      case 19 => "nineteenth"
      case 20 => "twentieth"
      case number =>
        number % 100 match {
          case 11 | 12 | 13 => s"${number}th" // now made redundant due to the above case statements, but left in in case we need it in the future
          case _ => number % 10 match {
            case 1 => s"${number}st"
            case 2 => s"${number}nd"
            case 3 => s"${number}rd"
            case _ => s"${number}th"
          }
        }
    }

}
