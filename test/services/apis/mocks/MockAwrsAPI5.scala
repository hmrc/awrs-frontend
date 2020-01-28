/*
 * Copyright 2020 HM Revenue & Customs
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

package services.apis.mocks

import connectors.mock.MockAWRSConnector
import models.SubscriptionTypeFrontEnd
import services.apis.AwrsAPI5
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits


trait MockAwrsAPI5 extends AwrsUnitTestTraits
  with MockAWRSConnector
  with MockSave4LaterService {

  val testAPI5: AwrsAPI5 = new AwrsAPI5(mockAWRSConnector, testSave4LaterService)

  def setupMockAwrsAPI5(apiSave4later: Option[SubscriptionTypeFrontEnd],
                        connector: MockConfiguration[SubscriptionTypeFrontEnd] = DoNotConfigure): Unit = {
    connector match {
      case Configure(subscriptionTypeFrontEnd) => setupMockAWRSConnectorWithOnly(lookupAWRSData = SubscriptionTypeFrontEnd.formats.writes(subscriptionTypeFrontEnd))
      case _ =>
    }
    setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = apiSave4later)
    setupMockSave4LaterServiceOnlySaveFunctions()
  }
}
