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

package services.mocks

import services.{AwrsApiDataCacheService, AwrsDataCacheService}
import org.mockito.Mockito._
import utils.AwrsUnitTestTraits

trait MockSave4LaterCacheService extends AwrsUnitTestTraits {
  // need to be lazy in case of overrides
  lazy val mockMainStoreSave4LaterCacheService: AwrsDataCacheService = mock[AwrsDataCacheService]
  lazy val mockApiSave4LaterCacheService: AwrsApiDataCacheService = mock[AwrsApiDataCacheService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMainStoreSave4LaterCacheService)
    reset(mockApiSave4LaterCacheService)
  }
}
