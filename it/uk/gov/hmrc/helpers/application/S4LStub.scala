
package uk.gov.hmrc.helpers.application

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json._
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.helpers.IntegrationSpec

trait S4LStub extends IntegrationSpec {

  case class SensitiveJs(override val decryptedValue: JsObject) extends Sensitive[JsObject]

  implicit lazy val jsonCrypto: Encrypter with Decrypter = new ApplicationCrypto(app.configuration.underlying).JsonCrypto
  implicit lazy val encryptionFormat: Writes[SensitiveJs] = JsonEncryption.sensitiveEncrypter[JsObject, SensitiveJs]

  def stubS4LGet(id: String, key: String = "", data: Option[JsObject] = None, scenarioState: Option[(String, String, String)] = None): StubMapping = {

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

    scenarioState match {
      case Some((scenario, precondition, endstate)) =>
        stubFor(get(urlMatching(s"/save4later/awrs-frontend/$id"))
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
        stubFor(get(urlMatching(s"/save4later/awrs-frontend/$id"))
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
