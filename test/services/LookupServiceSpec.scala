package services


import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import connectors.LookupConnector
import models.{AwrsEntry, AwrsStatus, Business, Info, SearchResult}
import utils.AwrsUnitTestTraits

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LookupServiceSpec extends AwrsUnitTestTraits {

  "LookupService" should {

    "call lookup connector and return a the response" in {
      val testAwrs = "XXAW00000123456"

      val data: JsValue = Json.toJson(Business(
        awrsRef = "XXAW00000123456",
        registrationDate = Some("2020-04-01"),
        status = AwrsStatus("Approved"),
        info = Info(businessName = Some("BusinessTest"),
          tradingName = Some("tradeName"),
          fullName = Some("fullName"),
          address = None),
        registrationEndDate = None
      )
      )

      val dataToReturn: Future[Option[SearchResult]] = SearchResult(List(AwrsEntry("Business", data)))

      val mockLookupConnector = mock[LookupConnector]

      when(mockLookupConnector.queryByUrn(any())(any(), any())).thenReturn(dataToReturn)
      val lookupService = new LookupService(mockLookupConnector)

      val resultWithData = lookupService.lookup(testAwrs)
      resultWithData mustBe dataToReturn

    }

    "call lookup connector and return an exception" in {
      val testAwrs = "XXAW00000123456"

      val dataToReturn: Future[Option[SearchResult]] = new Exception("test implicit exception thrown")

      val mockLookupConnector = mock[LookupConnector]

      when(mockLookupConnector.queryByUrn(any())(any(), any())).thenReturn(dataToReturn)
      val lookupService = new LookupService(mockLookupConnector)

      val resultWithData = lookupService.lookup(testAwrs)
      resultWithData mustBe dataToReturn

    }
  }
}