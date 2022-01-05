/*
 * Copyright 2022 HM Revenue & Customs
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

package builders

import java.util.UUID

import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import utils.AwrsSessionKeys

object SessionBuilder {

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId)
  }

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], userId: String, businessType: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId,
      "businessType" -> businessType,
      "businessName" -> "North East Wines")
  }

  def updateRequestWithSessionJSon(fakeRequest: FakeRequest[AnyContentAsJson], userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId)
  }

  def buildRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId,
      "businessName" -> "North East Wines"
    )
  }

  def buildRequestWithSession(userId: String, businessType: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId,
      "businessType" -> businessType,
      "businessName" -> "North East Wines"
    )
  }

  def buildRequestWithSession(userId: String, businessType: String, previousLocation: Option[String]) = {
    val sessionId = s"session-${UUID.randomUUID}"
    previousLocation match {
      case Some(location) => FakeRequest().withSession(
        "sessionId" -> sessionId,
        SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
        "userId"-> userId,
        AwrsSessionKeys.sessionPreviousLocation -> location,
        "businessType" -> businessType,
        "businessName" -> "North East Wines"
      )
      case _ => FakeRequest().withSession(
        "sessionId" -> sessionId,
        SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
        "userId"-> userId,
        "businessType" -> businessType,
        "businessName" -> "North East Wines"
      )
    }
  }

  def buildRequestWithSessionStartLocation(userId: String, businessType: String, startLocation: Option[String]) = {
    val sessionId = s"session-${UUID.randomUUID}"
    startLocation match {
      case Some(startLocation) => FakeRequest().withSession(
        "sessionId" -> sessionId,
        SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
        "userId"-> userId,
        AwrsSessionKeys.sessionJouneyStartLocation -> startLocation,
        "businessType" -> businessType,
        "businessName" -> "North East Wines"
      )
      case _ => FakeRequest().withSession(
        "sessionId" -> sessionId,
        SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
        "userId"-> userId,
        "businessType" -> businessType,
        "businessName" -> "North East Wines"
      )
    }
  }

  def buildRequestWithSessionNoUser() = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId)
  }
}
