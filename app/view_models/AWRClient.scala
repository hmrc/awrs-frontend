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

package view_models

import anorm.SqlParser._
import anorm._
import play.api.data.Forms._
import play.api.data._


case class Client(name: String, surname: String)
case class ClientWithId(id: Int, name: String, surname: String)

object AWRClient {
  val productForm = Form(mapping(
    "name" -> nonEmptyText,
    "surname" -> nonEmptyText)(Client.apply)(Client.unapply))

  val idForm = Form(
    "id" -> number(1))

  val clientParser = {
    get[Int]("id") ~
    get[String]("name") ~
    get[String]("surname") map {
      case id~name~surname => ClientWithId(id, name, surname)
    }
  }

  val clientWithoutIdParser = {
      get[String]("name") ~
      get[String]("surname") map {
        case name~surname => Client(name, surname)
      }
    }

  val idParser = {
    get[Int]("id") map {
      case id => id
    }
  }
}
