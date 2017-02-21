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

package forms

import forms.AWRSEnums.BooleanRadioEnum
import forms.prevalidation._
import forms.submapping.AddressMapping._
import forms.validation.util.ConstraintUtil.FormData
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import forms.validation.util.TargetFieldIds
import models.BusinessContacts
import play.api.data.Form
import play.api.data.Forms._

object BusinessContactsForm {

  val contactAddressSame = "contactAddressSame"
  val contactAddress = "contactAddress"
  val contactFirstName = "contactFirstName"
  val contactLastName = "contactLastName"
  val email = "email"
  val confirmEmail = "confirmEmail"
  val telephone = "telephone"

  private val bothEmailsMatch = {
    val ifEmailsDoNotMatch = (data: FormData) => !data.getOrElse(email, "").equals(data.getOrElse(confirmEmail, ""))
    CrossFieldConstraint(
      ifEmailsDoNotMatch,
      simpleCrossFieldErrorMessage(TargetFieldIds(confirmEmail), "awrs.generic.error.emails_do_not_match"))
  }

  private val contactAddress_compulsory = yesNoQuestion_compulsory("contactAddressSame", "awrs.business_contacts.error.contact_address_same_empty")
  private val whenContactAddressNo = whenAnswerToFieldIs("contactAddressSame", BooleanRadioEnum.No.toString)(_)

  val businessContactsValidationForm = Form(mapping(
    contactFirstName -> firstName_compulsory(contactFirstName),
    contactLastName -> lastName_compulsory(contactLastName),
    telephone -> telephone_compulsory(),
    email -> email_compulsory(fieldId = email),
    confirmEmail -> (confirmEmail_compulsory(fieldId = confirmEmail) + bothEmailsMatch),
    contactAddressSame -> contactAddress_compulsory,
    contactAddress -> (ukAddress_compulsory(prefix = contactAddress, prefixRefNameInErrorMessage = "contact").toOptionalAddressMapping iff whenContactAddressNo),
    "modelVersion" -> ignored[String](BusinessContacts.latestModelVersion)
  )(BusinessContacts.apply)(BusinessContacts.unapply))

  val businessContactsForm = PreprocessedForm(businessContactsValidationForm)

}