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

package uk.gov.hmrc.helpers.application

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json._
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.helpers.IntegrationSpec
import crypto.CryptoProvider

trait S4LStub extends IntegrationSpec {

  case class SensitiveJs(override val decryptedValue: JsObject) extends Sensitive[JsObject]

  implicit lazy val jsonCrypto: Encrypter with Decrypter = new CryptoProvider(app.configuration).crypto
  implicit lazy val encryptionFormat: Writes[SensitiveJs] = JsonEncryption.sensitiveEncrypter[JsObject, SensitiveJs]

  def stubS4LGet(id: String, key: String = "", data: Option[JsObject] = None, scenarioState: Option[(String, String, String)] = None, api: Boolean = false): StubMapping = {

    val keyEncryptor = JsonEncryption.sensitiveEncrypter[String, SensitiveString]
    val encKey = keyEncryptor.writes(SensitiveString(key)).as[String]

    val encData = data.map(dt => encryptionFormat.writes(SensitiveJs(dt)).as[JsString])

    val s4LResponse = encData match {
      case Some(s4lEncData) => Json.obj(
        "id" -> encKey,
        "data" -> Json.obj(key -> s4lEncData)
      )
      case _                => Json.obj()
    }

    val apiString = if (api) "awrs-frontend-api" else "awrs-frontend"

    scenarioState match {
      case Some((scenario, precondition, endstate)) =>
        stubFor(get(urlMatching(s"/save4later/$apiString/$id"))
          .inScenario(scenario)
          .whenScenarioStateIs(precondition)
          .willReturn(
            aResponse().
              withStatus(encData.fold(404)(_ => 200)).
              withBody(s4LResponse.toString())
          )
          .willSetStateTo(endstate)
        )
      case _                       =>
        stubFor(get(urlMatching(s"/save4later/$apiString/$id"))
          .inScenario("")
          .willReturn(
            aResponse().
              withStatus(encData.fold(404)(_ => 200)).
              withBody(s4LResponse.toString())
          )
      )
    }
  }

  def stubS4LPut(id: String, key: String, data: JsObject, api: Boolean = false): StubMapping = {

    val encData = encryptionFormat.writes(SensitiveJs(data))

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    val apiString = if (api) "awrs-frontend-api" else "awrs-frontend"

    stubFor(put(urlMatching(s"/save4later/$apiString/$id/data/$key"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }
}
