/*
 * Copyright 2019 HM Revenue & Customs
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

package services.helper

import models._

object AddressComparator {

  def isDifferent(toAddress: Option[Address], fromAddress: Option[Address]): Boolean =
    (toAddress, fromAddress) match {
      case (Some(t), Some(f)) => !toAddress.equals(fromAddress)
      case (None, None) => false
      case _ => true
    }

}
