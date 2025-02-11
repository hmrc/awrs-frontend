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

package forms

import forms.HaveYouRegisteredForm.{hasUserRegistered, haveYouRegisteredForm}
import forms.test.util.AwrsFormTestUtils
import models.HaveYouRegisteredModel
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form

class HaveYouRegisteredFormTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach with AwrsFormTestUtils {

  "HaveYouRegisteredForm" should {
    implicit val form: Form[HaveYouRegisteredModel] = haveYouRegisteredForm.form
    val fieldId = "hasUserRegistered"


    "field is left blank" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.enrolment.have_you_registered.error"
        },
        _ => fail("Field should contain errors")
      )
    }

    "allow valid submissions" in {
      assertFormIsValid(form, Map(hasUserRegistered -> "true"))
      assertFormIsValid(form, Map(hasUserRegistered -> "false"))
    }
  }
}
