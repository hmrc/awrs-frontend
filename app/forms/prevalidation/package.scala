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

import play.api.data.Form

package object prevalidation {

  import TrimOption._

  val defaultTrims: Map[String, TrimOption] = Map[String, TrimOption](
    "NINO" -> all,
    "nino" -> all,
    "passportNumber" -> all,
    "nationalID" -> all,
    "vrn" -> all,
    "vatNumber" -> all,
    "companyRegNumber" -> all,
    "companyRegistrationNumber" -> all,
    "utr" -> utr,
    "postcode" -> all
  )

  import CaseOption._

  val defaultCases: Map[String, CaseOption] = Map[String, CaseOption](
    "NINO" -> upper,
    "nino" -> upper,
    "passportNumber" -> upper,
    "nationalID" -> upper,
    "vrn" -> upper,
    "vatNumber" -> upper,
    "companyRegNumber" -> upper,
    "companyRegistrationNumber" -> upper,
    "utr" -> upper,
    "postcode" -> upper
  )

  val trimAllFunc: String => String = (value: String) => value.replaceAll("[\\s]", "")
  val trimBothFunc: String => String = (value: String) => value.trim
  val trimBothAndCompressFunc: String => String = (value: String) => value.trim.replaceAll("[\\s]{2,}", " ")
  val trimUTR: String => String = (value: String) =>  value
  val trimCRN: String => String = (value: String) => if(value.length == 7) "0" + value else value

  def PreprocessedForm[T](validation: Form[T], trimRules: Map[String, TrimOption] = defaultTrims, caseRules: Map[String, CaseOption] = defaultCases): PrevalidationAPI[T] = {
    val trules = trimRules
    val crules = caseRules
    new PrevalidationAPI[T] {
      override val formValidation: Form[T] = validation
      override val trimRules: Map[String, TrimOption] = trules
      override val caseRules: Map[String, CaseOption] = crules
    }
  }

}
