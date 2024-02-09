/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.helpers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers.OK
import uk.gov.hmrc.helpers.http.StubbedBasicHttpCalls

trait AuthHelpers extends StubbedBasicHttpCalls {

  def mockAuthedCall: StubMapping = {
    stubbedPost("/write/audit", OK, """{"x":2}""")
    stubbedPost("/write/audit/merged", OK, """{"x":2}""")
    stubbedPost("/auth/authorise", OK, """{}""")
  }

}
