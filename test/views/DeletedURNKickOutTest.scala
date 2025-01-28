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

package views

import play.twirl.api.HtmlFormat
import views.html.deleted_urn_kickout
class DeletedURNKickOutTest extends ViewTestFixture {

  val view: deleted_urn_kickout =
    app.injector.instanceOf[views.html.deleted_urn_kickout]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)
  "deleted_urn_kickout page" should {
    "render the correct content" in {
      heading mustBe "Get Help with your AWRS registration"
      bodyText mustBe "Contact HMRC Phone: 020 7946 0101 Textphone: 020 7946 0102 Monday to Friday, 9am to 5pm (except public holidays) Welsh language = 020 7946 0103 Monday to Friday, 8:30am to 5pm"
    }
  }
}