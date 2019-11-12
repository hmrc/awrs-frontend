
package uk.gov.hmrc.helpers.http

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig.ResponseBodyPartFactory

trait StubbedBasicHttpCalls {

  def stubbedHead(url: String, statusCode: Int): StubMapping = {
    stubFor(head(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
      )
    )
  }

  def stubbedGet(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(get(urlPathMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def  stubbedGetQueryParams(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(get(urlPathEqualTo(url))
      .withQueryParam("encoding-type", equalTo("url"))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedPost(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedPut(url: String, statusCode: Int): StubMapping = {
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(
            """{
              |"id": "xxx",
              |"data": {}
              |}""".stripMargin)
      )
    )
  }

  def stubbedPatch(url: String, statusCode: Int, responseBody: String = ""): StubMapping = {
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedDelete(url: String, statusCode: Int): StubMapping = {
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
      )
    )
  }
}
