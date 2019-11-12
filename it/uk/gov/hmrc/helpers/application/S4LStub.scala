
package uk.gov.hmrc.helpers.application

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryptor
import uk.gov.hmrc.crypto.{CryptoWithKeysFromConfig, Protected}

trait S4LStub {

  def stubS4LGet(id: String, key: String = "", data: Option[JsObject] = None, scenarioState: Option[(String, String, String)] = None)(implicit jsonCrypto: CryptoWithKeysFromConfig,
                                                                  encryptionFormat: JsonEncryptor[JsObject]): StubMapping = {

    implicit lazy val encryptionFormatString: JsonEncryptor[JsString] = new JsonEncryptor[JsString]()

    val keyEncryptor = new JsonEncryptor[String]()
    val encKey = keyEncryptor.writes(Protected(key)).as[String]

    val encData = data.map(dt => encryptionFormat.writes(Protected(dt)).as[JsString])

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

  def stubS4LPut(id: String, key: String, data: JsObject)(implicit jsonCrypto: CryptoWithKeysFromConfig,
                                                          encryptionFormat: JsonEncryptor[JsObject]
                                                          ): StubMapping = {

    val encData = encryptionFormat.writes(Protected(data))

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(put(urlMatching(s"/save4later/awrs-frontend/$id/data/$key"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }
}
