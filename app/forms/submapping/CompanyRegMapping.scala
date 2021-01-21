/*
 * Copyright 2021 HM Revenue & Customs
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

package forms.submapping

import forms.AWRSEnums.BooleanRadioEnum
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI.MappingUtil
import forms.validation.util.NamedMappingAndUtil._
import models.{CompanyRegDetails, TupleDate}
import play.api.data.Forms._
import play.api.data.Mapping


object CompanyRegMapping {

  import TupleDateMapping._

  val crn = "companyRegistrationNumber"
  val dateOfIncorporation = "dateOfIncorporation"

  val whenAnsweredYesToCRN = (doYouHaveCRNFieldId: String) =>
    (data: Map[String, String]) =>
      whenAnswerToIdTypeIs(doYouHaveCRNFieldId, BooleanRadioEnum.Yes)(data)

  def dateOfIncorporation_compulsory: Mapping[TupleDate] =
    tupleDate_compulsory(
      isEmptyErrMessage = simpleErrorMessage(_, "awrs.generic.error.companyRegDate_empty"),
      isInvalidErrMessage = simpleErrorMessage(_, "awrs.generic.error.companyRegDate_invalid"),
      isTooEarlyCheck = None,
      isTooLateCheck = None)

  // Reusable company reg mapping
  def companyReg_compulsory(prefix: String): Mapping[CompanyRegDetails] = mapping(
    crn -> crn_compulsory(fieldId = prefix attach crn).toStringFormatter,
    dateOfIncorporation -> dateOfIncorporation_compulsory
  )(CompanyRegDetails.apply)(CompanyRegDetails.unapply)


  implicit class CompanyRegUtil(companyRegMapping: Mapping[CompanyRegDetails]) {
    def toOptionalCompanyRegMapping: Mapping[Option[CompanyRegDetails]] =
      companyRegMapping.transform[Option[CompanyRegDetails]](
        (value: CompanyRegDetails) => Some(value),
        (value: Option[CompanyRegDetails]) => value.getOrElse(CompanyRegDetails("", TupleDate("", "", ""))))
  }

}
