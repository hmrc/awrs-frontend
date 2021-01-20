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

package services.mocks

import models.SectionChangeIndicators
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.ApplicationService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future

trait MockApplicationService extends AwrsUnitTestTraits {

  val mockApplicationService = mock[ApplicationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationService)
  }

  import MockApplicationService._

  protected final def setupMockApplicationService(hasAPI5ApplicationChanged: Boolean = defaultTrueBoolean,
                                                  getApi5ChangeIndicators: SectionChangeIndicators = defaultSectionChangeIndicators): Unit = {
    when(mockApplicationService.hasAPI5ApplicationChanged(ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(hasAPI5ApplicationChanged))
    when(mockApplicationService.getApi5ChangeIndicators(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(getApi5ChangeIndicators))
  }
}

object MockApplicationService {
  val defaultTrueBoolean = true
  val defaultSectionChangeIndicators: SectionChangeIndicators = getSectionChangeIndicators()
}
