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

package services.mocks

import models.BusinessCustomerDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.BusinessCustomerService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future


trait MockBusinessCustomerService extends AwrsUnitTestTraits {

  val mockBusinessCustomerService = mock[BusinessCustomerService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBusinessCustomerService)
  }

  import MockBusinessCustomerService._

  protected final def setupMockBusinessCustomerService(getReviewBusinessDetails: Option[BusinessCustomerDetails] = defaultBusinessCustomerDetails): Unit =
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(getReviewBusinessDetails))

  protected final def verifyBusinessCustomerService(getReviewBusinessDetails: Option[Int] = None): Unit =
    getReviewBusinessDetails ifDefinedThen (count => verify(mockBusinessCustomerService, times(count)).getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any() ))

}

object MockBusinessCustomerService {
  val defaultBusinessCustomerDetails: BusinessCustomerDetails = testBusinessCustomerDetails("SOP")
}
