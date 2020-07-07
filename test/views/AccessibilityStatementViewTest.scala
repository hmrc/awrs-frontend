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

package views

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class AccessibilityStatementViewTest extends UnitSpec with GuiceOneAppPerSuite {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)

  "Accessibility Statement" when {

    "Html content should" should {

      lazy val view = views.html.awrs_accessibility_statement("/alcohol-wholesale-scheme/index")(request, messages, mockAppConfig)
      lazy val document: Document = Jsoup.parse(view.body)

      "correctly display title and headings" in {

      document.select("head title").text should be (
        "Accessibility statement for Alcohol Wholesaler Registration Scheme - Register as an alcohol wholesaler or producer - GOV.UK")
      document.select("#heading").text should be (
        "Accessibility statement for Alcohol Wholesaler Registration Scheme")
      document.select("#heading-using-this-service").text should be (
        "Using this service")
      document.select("#heading-how-accessible").text should be (
        "How accessible this service is")
      document.select("#heading-reporting-problems").text should be (
        "Reporting accessibility problems with this service")
      document.select("#heading-complaints").text should be (
        "What to do if you are not happy with how we respond to your complaint")
      document.select("#heading-contact-us").text should be (
        "Contacting us by phone or getting a visit from us in person")
      document.select("#heading-technical-info").text should be (
        "Technical information about this service’s accessibility")
      document.select("#heading-how-we-tested").text should be (
        "How we tested this service")
      }

      "correctly display link keys" in {

      document.select("#introduction > p:nth-child(3) > a").text should be (
        "accessibility statement")
      assert(document.select("#introduction > p:nth-child(3) > a")
        .attr("href") === "https://www.gov.uk/help/accessibility")

      document.select("#introduction > p:nth-child(4) > a").text should be (
        "https://www.gov.uk/guidance/the-alcohol-wholesaler-registration-scheme-awrs")
      assert(document.select("#introduction > p:nth-child(4) > a")
        .attr("href") === "https://www.gov.uk/guidance/the-alcohol-wholesaler-registration-scheme-awrs")

      document.select("#using-this-service > p:nth-child(7) > a").text should be (
        "AbilityNet")
      assert(document.select("#using-this-service > p:nth-child(7) > a")
        .attr("href") === "https://mcmw.abilitynet.org.uk/")

      document.select("#how-accessible > p:nth-child(2) > a").text should be (
        "Web Content Accessibility Guidelines version 2.1 AA standard")
      assert(document.select("#how-accessible > p:nth-child(2) > a")
        .attr("href") === "https://www.w3.org/TR/WCAG21/")

      document.select("#reporting-problems > p > a").text should be (
        "hmrc-accessibility-problems@digital.hmrc.gov.uk")
      assert(document.select("#reporting-problems > p > a")
        .attr("href") === "mailto:hmrc-accessibility-problems@digital.hmrc.gov.uk")

      document.select("#complaints > p > a:nth-child(1)").text should be (
        "contact the Equality Advisory and Support Service")
      assert(document.select("#complaints > p > a:nth-child(1)")
        .attr("href") === "https://www.equalityadvisoryservice.com/")

      document.select("#complaints > p > a:nth-child(2)").text should be (
        "Equality Commission for Northern Ireland")
      assert(document.select("#complaints > p > a:nth-child(2)")
        .attr("href") === "https://www.equalityni.org/Home")

      document.select("#contact-us > p:nth-child(4) > a").text should be (
        "contact us")
      assert(document.select("#contact-us > p:nth-child(4) > a")
        .attr("href") === "https://www.gov.uk/dealing-hmrc-additional-needs/deaf-hearing-impaired")

      document.select("#technical-information > p:nth-child(3) > a").text should be (
        "Web Content Accessibility Guidelines version 2.1 AA standard")
      assert(document.select("#technical-information > p:nth-child(3) > a")
        .attr("href") === "https://www.w3.org/TR/WCAG21/")

      document.select("#how-we-tested > p:nth-child(3) > a").text should be (
        "Digital Accessibility Centre")
      assert(document.select("#how-we-tested > p:nth-child(3) > a")
        .attr("href") === "http://www.digitalaccessibilitycentre.org/")
      }

      "correctly display Introduction paragraph text" in {

        document.select("#statement_description").text should be (
          "This accessibility statement explains how accessible this service is, " +
            "what to do if you have difficulty using it, and how to report accessibility problems with the service.")
        document.select("#introduction > p:nth-child(3)").text should be (
          "This service is part of the wider GOV.UK website. " +
            "There is a separate accessibility statement for the main GOV.UK website.")
        document.select("#introduction > p:nth-child(4)").text should be (
          "This page only contains information about the Alcohol Wholesale Registration Service, " +
            "available at https://www.gov.uk/guidance/the-alcohol-wholesaler-registration-scheme-awrs.")
      }

      "correctly display Using this service paragraph text" in {

        document.select("#using-this-service > p:nth-child(2)").text should be (
          "All businesses that sell, or arranges the sale of alcohol, " +
            "can sign up to use compatible software to submit an application for approval. Placeholder for content:")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(1)").text should be (
          "Placeholder Bullet 1")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(2)").text should be (
          "Placeholder Bullet 2")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(3)").text should be (
          "Placeholder Bullet 3")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(4)").text should be (
          "Placeholder Bullet 4")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(5)").text should be (
          "Placeholder Bullet 5")
        document.select("#using-this-service > ul:nth-child(3) > li:nth-child(6)").text should be (
          "Placeholder Bullet 6")
        document.select("#using-this-service > p:nth-child(4)").text should be (
          "This service is run by HM Revenue and Customs (HMRC). " +
            "We want as many people as possible to be able to use this service. This means you should be able to:")
        document.select("#using-this-service > ul:nth-child(5) > li:nth-child(1)").text should be (
          "change colours, contrast levels and fonts")
        document.select("#using-this-service > ul:nth-child(5) > li:nth-child(2)").text should be (
          "zoom in up to 300% without the text spilling off the screen")
        document.select("#using-this-service > ul:nth-child(5) > li:nth-child(3)").text should be (
          "get from the start of the service to the end using just a keyboard")
        document.select("#using-this-service > ul:nth-child(5) > li:nth-child(4)").text should be (
          "get from the start of the service to the end using speech recognition software")
        document.select("#using-this-service > ul:nth-child(5) > li:nth-child(5)").text should be (
          "listen to the service using a screen reader (including the most recent versions of JAWS, NVDA and VoiceOver)")
        document.select("#using-this-service > p:nth-child(6)").text should be (
          "We have also made the text in this service as simple as possible to understand.")
        document.select("#using-this-service > p:nth-child(7)").text should be (
          "AbilityNet has advice on making your device easier to use if you have a disability.")
      }

      "correctly display How accessible this service is paragraph text" in {

        document.select("#how-accessible > p:nth-child(2)").text should be (
          "This service is fully compliant with the Web Content Accessibility Guidelines version 2.1 AA standard.")
        document.select("#how-accessible > p:nth-child(3)").text should be (
          "There are no known accessibility issues within this service.")
      }

      "correctly display Reporting accessibility problems with this service paragraph text" in {

        document.select("#reporting-problems > p").text should be (
          "We are always looking to improve the accessibility of this service. " +
            "If you find any problems that are not listed on this page or think we are not meeting accessibility " +
            "requirements, contact hmrc-accessibility-problems@digital.hmrc.gov.uk.")
      }

      "correctly display What to do if you are not happy with how we respond to your complaint paragraph text" in {

        document.select("#complaints > p").text should be (
          "The Equality and Human Rights Commission (EHRC) is responsible for enforcing the Public Sector Bodies " +
            "(Websites and Mobile Applications) (No.2) Accessibility Regulations 2018 " +
            "(the ‘accessibility regulations’). If you are happy with how we respond to your complaint, " +
            "contact the Equality Advisory and Support Service (EASS), or the Equality Commission for " +
            "Northern Ireland (ECNI) if you live in Northern Ireland.")
      }

      "correctly display Contacting us by phone or getting a visit from us in person paragraph text" in {

        document.select("#contact-us > p:nth-child(2)").text should be (
          "We provide a text relay service if you are deaf, hearing impaired or have a speech impediment.")
        document.select("#contact-us > p:nth-child(3)").text should be (
          "We can provide a British Sign Language (BSL) interpreter, or you can arrange a visit from a " +
            "HMRC advisor to help you complete the service.")
        document.select("#contact-us > p:nth-child(4)").text should be (
          "Find out how to contact us.")
      }

      "correctly display Technical information about this service’s accessibility paragraph text" in {

        document.select("#technical-information > p:nth-child(2)").text should be (
          "HMRC is committed to making this service accessible, in accordance with the Public Sector Bodies " +
            "(Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018.")
        document.select("#technical-information > p:nth-child(3)").text should be (
            "This service is fully compliant with the Web Content Accessibility Guidelines version 2.1 AA standard.")
      }

      "correctly display How we tested this service paragraph text" in {

        document.select("#how-we-tested > p:nth-child(2)").text should be (
          "The service was last tested on 6 July 2020 and was checked for compliance with WCAG 2.1 AA.")
        document.select("#how-we-tested > p:nth-child(3)").text should be (
          "The service was built using parts that were tested by the Digital Accessibility Centre. " +
            "The full service was tested by HMRC and included disabled users.")
        document.select("#how-we-tested > p:nth-child(4)").text should be (
          "This page was prepared on 6 July 2020. It was last updated on 6 July 2020.")
      }
    }
  }
}
