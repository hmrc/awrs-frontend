/*
 * Copyright 2025 HM Revenue & Customs
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

import scala.concurrent.Future
import scala.language.implicitConversions


trait ImplicitConversions {
  implicit def optionUtil[T, S <: T](data: S): Option[T] = Some(data)

  implicit def futureUtil[T, S <: T](data: S): Future[T] = Future.successful(data)

  implicit def futureUtil2[T, S <: T](data: S): Future[Option[T]] = Future.successful(Some(data))
}

object ImplicitConversions extends ImplicitConversions