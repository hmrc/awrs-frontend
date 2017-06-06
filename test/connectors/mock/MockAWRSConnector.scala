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

package connectors.mock

import connectors.AWRSConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import utils.AwrsUnitTestTraits
import utils.TestUtil._
import utils.AwrsTestJson._

import scala.concurrent.Future


trait MockAWRSConnector extends AwrsUnitTestTraits {

  // need to be lazy incase of overrides
  lazy val mockAWRSConnector = mock[AWRSConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAWRSConnector)
  }

  import MockAWRSConnector._

  def setupMockAWRSConnector(lookupAWRSData: JsValue = defaultAPI5,
                             checkStatus: SubscriptionStatusType = defaultSubscriptionStatusType,
                             getStatusInfo: StatusInfoType = defaultStatusTypeInfo,
                             submitAWRSData: SuccessfulSubscriptionResponse = defaultSuccessfulSubscriptionResponse,
                             updateAWRSData: SuccessfulUpdateSubscriptionResponse = defaultSuccessfulUpdateSubscriptionResponse
                            ) =
    setupMockAWRSConnectorWithOnly(
      lookupAWRSData = lookupAWRSData,
      checkStatus = checkStatus,
      getStatusInfo = getStatusInfo,
      submitAWRSData = submitAWRSData,
      updateAWRSData = updateAWRSData
    )

  def setupMockAWRSConnectorWithOnly(
                                      lookupAWRSData: MockConfiguration[JsValue] = DoNotConfigure,
                                      checkStatus: MockConfiguration[SubscriptionStatusType] = DoNotConfigure,
                                      getStatusInfo: MockConfiguration[StatusInfoType] = DoNotConfigure,
                                      submitAWRSData: MockConfiguration[SuccessfulSubscriptionResponse] = DoNotConfigure,
                                      updateAWRSData: MockConfiguration[SuccessfulUpdateSubscriptionResponse] = DoNotConfigure,
                                      updateGroupBusinessPartner: MockConfiguration[SuccessfulUpdateGroupBusinessPartnerResponse] = DoNotConfigure
                                    ) = {
    lookupAWRSData ifConfiguredThen (jsValue => when(mockAWRSConnector.lookupAWRSData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(jsValue)))
    checkStatus ifConfiguredThen (status => when(mockAWRSConnector.checkStatus(Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(status)))
    getStatusInfo ifConfiguredThen (info => when(mockAWRSConnector.getStatusInfo(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(info)))
    submitAWRSData ifConfiguredThen (data => when(mockAWRSConnector.submitAWRSData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(data)))
    updateAWRSData ifConfiguredThen (data => when(mockAWRSConnector.updateAWRSData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(data)))
    updateGroupBusinessPartner ifConfiguredThen (data => when(mockAWRSConnector.updateGroupBusinessPartner(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(data)))
  }

  def verifyAWRSConnector(lookupAWRSData: Option[Int] = None,
                          checkStatus: Option[Int] = None,
                          getStatusInfo: Option[Int] = None
                         ) = {
    lookupAWRSData ifDefinedThen (count => verify(mockAWRSConnector, times(count)).lookupAWRSData(Matchers.any())(Matchers.any(), Matchers.any()))
    checkStatus ifDefinedThen (count => verify(mockAWRSConnector, times(count)).checkStatus(Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any()))
    getStatusInfo ifDefinedThen (count => verify(mockAWRSConnector, times(count)).getStatusInfo(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
  }


}

object MockAWRSConnector {
  // default test data
  // for mockAWRSConnector
  val defaultAPI5 = api5LTDJson
  val defaultSubscriptionStatusType = testSubscriptionStatusTypeApprovedWithConditions
  val defaultStatusTypeInfo = testStatusInfoTypeApprovedWithConditions
  val defaultSuccessfulSubscriptionResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val defaultSuccessfulUpdateSubscriptionResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")

}
