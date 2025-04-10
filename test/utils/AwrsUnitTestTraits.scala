/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import audit.Auditable
import config.ApplicationConfig
import connectors.LookupConnector
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc._
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents, stubMessages, stubMessagesApi}
import services.{BusinessDetailsService, BusinessMatchingService, DeEnrolService, LookupService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.test.Helpers.{defaultAwaitTimeout, await => helperAwait}
import views.html.helpers.awrsErrorNotFoundTemplate
import views.html.{awrs_application_error, error_template, unauthorised}
import views.html.view_application.subviews.subview_delete_confirmation

import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait AwrsUnitTestTraits extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  private val messagesActionBuilder: MessagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  private val stubCC = stubControllerComponents()

  lazy val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    stubCC.parsers,
    stubMessagesApi(),
    stubCC.langs,
    stubCC.fileMimeTypes,
    ExecutionContext.global
  )

  val mockAwrsFeatureSwitches:AWRSFeatureSwitches = mock[AWRSFeatureSwitches]
  val mockDeEnrolService: DeEnrolService = mock[DeEnrolService]
  val mockAccountUtils: AccountUtils = mock[AccountUtils]
  val mockBusinessDetailsService: BusinessDetailsService = mock[BusinessDetailsService]
  val mockAuditable: Auditable = mock[Auditable]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockCountryCodes: CountryCodes = mock[CountryCodes]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockMessages: Messages = mock[Messages]
  val mockLookupConnector:LookupConnector = mock[LookupConnector]
  val testLookupService:LookupService = new LookupService(mockLookupConnector)
  val mockMatchingService: BusinessMatchingService = mock[BusinessMatchingService]
  implicit val messages: Messages = stubMessages()

  def await[A](result: Future[A]): A = {
    helperAwait(result)
  }

  when(mockAppConfig.countryCodes)
    .thenReturn(mockCountryCodes)
  when(mockCountryCodes.countries)
    .thenReturn(
      """[
        |"United Kingdom"
        |]""".stripMargin)
  when(mockCountryCodes.getCountryCode(any()))
    .thenReturn(Some("ES"))
  when(mockCountryCodes.getSupplierAddressWithCountry(any()))
    .thenReturn(Some(TestUtil.testAddressInternational))

  lazy val mockUnauthorised: unauthorised = app.injector.instanceOf[views.html.unauthorised]
  lazy val mockDeleteConfirm: subview_delete_confirmation = app.injector.instanceOf[views.html.view_application.subviews.subview_delete_confirmation]
  lazy val mockAppError: awrs_application_error = app.injector.instanceOf[views.html.awrs_application_error]
  lazy val mockNotFound: awrsErrorNotFoundTemplate = app.injector.instanceOf[views.html.helpers.awrsErrorNotFoundTemplate]
  lazy val mockError: error_template = app.injector.instanceOf[views.html.error_template]

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockAppConfig.templateUnauthorised)
      .thenReturn(mockUnauthorised)
    when(mockAppConfig.templateAppError)
      .thenReturn(mockAppError)
    when(mockAppConfig.templateDeleteConfirm)
      .thenReturn(mockDeleteConfirm)
    when(mockAppConfig.templateNotFound)
      .thenReturn(mockNotFound)
    when(mockAppConfig.templateError)
      .thenReturn(mockError)
  }

  lazy val allEntities = List("SOP", "LTD", "Partnership", "LLP", "LTD_GRP", "LLP_GRP")
  lazy val directorEntities = List("LTD", "LTD_GRP")
  lazy val partnerEntities = List("Partnership", "LLP", "LLP_GRP")
  lazy val groupEntities = List("LTD_GRP", "LLP_GRP")

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  implicit def convertToOption[T](value: T): Option[T] = Some(value)

  implicit def convertToFuture[T](value: T): Future[Option[T]] = Future.successful(value)

  implicit def convertToFuture[T](err: Throwable): Future[Option[T]] = Future.failed(err)

  // used to help mock setup functions to clarify if certain results must be mocked.
  sealed trait MockConfiguration[+A] {
    final def get: A = this match {
      case Configure(config) => config
      case _ => throw new RuntimeException("This element is not to be configured")
    }

    final def ifConfiguredThen(action: A => Unit): Unit = this match {
      case Configure(dataToReturn) => action(dataToReturn)
      case _ =>
    }
  }

  def setupEnrolmentJourneyFeatureSwitchMock(value:Boolean): Unit = {
    when(mockAwrsFeatureSwitches.enrolmentJourney()).thenReturn(BooleanFeatureSwitch("enrolmentJourney", value))
  }

  case class Configure[A](config: A) extends MockConfiguration[A]

  case object DoNotConfigure extends MockConfiguration[Nothing]

  implicit def convertToMockConfiguration[T](value: T): MockConfiguration[T] = Configure(value)

  implicit def convertToMockConfiguration2[T](value: T): MockConfiguration[Option[T]] = Configure(value)

  implicit def convertToMockConfiguration3[T](value: T): MockConfiguration[Future[T]] = Configure(Future.successful(value))

  implicit def convertToMockConfiguration4[T](value: T): MockConfiguration[Future[Option[T]]] = Configure(Future.successful(Some(value)))

  implicit def convertToMockConfiguration5[T](err: Throwable): MockConfiguration[Future[Option[T]]] = Configure(err)


  // used to for mock setup functions to specify the location of the data,
  // CachedLocally for when the data can be found in KeyStore or Save4Later, and NotCachedLocally where
  // a call to another service is required
  sealed trait CacheConfigurationLocation

  case object CachedLocally extends CacheConfigurationLocation

  case object NotCachedLocally extends CacheConfigurationLocation

  implicit class VerificationUtil(someCount: Option[Int]) {
    // util function designed for aiding verify functions
    def ifDefinedThen(action: Int => Unit): Unit = someCount match {
      case Some(count) => action(count)
      case _ =>
    }
  }

  case class Ids(utr: Boolean, nino: Boolean, crn: Boolean, vrn: Boolean)

  def getIds(entityType: String): Ids = entityType match {
    case "SOP" => Ids(utr = true, nino = true, crn = false, vrn = true)
    case "Partnership" => Ids(utr = true, nino = false, crn = false, vrn = true)
    case _ => Ids(utr = true, nino = false, crn = true, vrn = true)
  }

  def testId(shouldExist: Boolean)(targetFieldId: String)(implicit doc: Document): Assertion = shouldExist match {
    case false => doc.getElementById(targetFieldId) mustBe null
    case true => doc.getElementById(targetFieldId) must not be null
  }

  def testText(expectedText: String)(targetFieldId: String)(implicit doc: Document): Assertion = {
    doc.getElementById(targetFieldId).text mustBe expectedText
  }

}
