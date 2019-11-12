
package uk.gov.hmrc.helpers.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

trait WireMockSetup {
  self: WireMockConfig =>

  private val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  protected def startWmServer(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  protected def stopWmServer(): Unit = {
    wireMockServer.stop()
  }

  protected def resetWmServer(): Unit = {
    wireMockServer.resetAll()
    WireMock.reset()
  }
}
