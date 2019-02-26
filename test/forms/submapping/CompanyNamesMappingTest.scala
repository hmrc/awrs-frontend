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

package forms.submapping

import forms.test.util._
import models.CompanyNames
import play.api.data.Form
import play.api.data.Forms._
import utils.AwrsUnitTestTraits

class CompanyNamesMappingTest extends AwrsUnitTestTraits {

  import CompanyNamesMapping._

  private case class TestForm(sub: CompanyNames)

  val prefix = "prefix"

  private def testForm(validateCompanyName: Boolean) = Form(mapping(
    prefix -> companyNamesMapping(prefix, FormData => validateCompanyName)
  )(TestForm.apply)(TestForm.unapply))


  "CompanyNamesMapping" should {
    "When company name is validated" in {
      implicit val f = testForm(validateCompanyName = true)
      NamedUnitTests.companyNamesAreValid(idPrefix = prefix, isBusinessNameRequired = true)
    }

    "When company name is not validated" in {
      implicit val f = testForm(validateCompanyName = false)
      NamedUnitTests.companyNamesAreValid(idPrefix = prefix, isBusinessNameRequired = false)
    }
  }


}
