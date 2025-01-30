package models

import models.AwrsStatus.{Approved, DeRegistered, Revoked}
import play.api.libs.json.Json
import utils.AwrsUnitTestTraits


class AwrsEntryTest extends AwrsUnitTestTraits {

  val testInfo: Info = Info("testBusinessName", "testTradingName", "testFullName",
    Address("testline1", "testline2", "testline3", "testline4", "testPostCode", "testCountry"))

  "AwrsEntry" should {
    "Correctly convert Business to json and back with status Approved" in {
      val testObj: AwrsEntry = Business(
        awrsRef = "testValue",
        registrationDate = "01/01/1970",
        status = Approved,
        registrationEndDate = "01/01/2017",
        info = testInfo
      )
      val json = Json.toJson[AwrsEntry](testObj)

      val convBack = Json.fromJson[AwrsEntry](json)
      convBack.get mustBe testObj
    }

    "Correctly convert Business to json and back with status DeRegistered" in {
      val testObj: AwrsEntry = Business(
        awrsRef = "testValue",
        registrationDate = "01/01/1970",
        status = DeRegistered,
        registrationEndDate = "01/01/2017",
        info = testInfo
      )
      val json = Json.toJson[AwrsEntry](testObj)

      val convBack = Json.fromJson[AwrsEntry](json)
      convBack.get mustBe testObj
    }

    "Correctly convert Group to json and back with status Revoked" in {
      val testObj: AwrsEntry = Group(
        awrsRef = "testValue",
        registrationDate = "01/01/1970",
        status = Revoked,
        registrationEndDate = "01/01/2017",
        info = testInfo,
        members = List(
          testInfo.copy(businessName = "testBusinessName2"),
          testInfo.copy(businessName = "testBusinessName3")
        )
      )
      val json = Json.toJson[AwrsEntry](testObj)

      val convBack = Json.fromJson[AwrsEntry](json)
      convBack.get mustBe testObj
    }
  }

  "AwrsStatus" should {
    "Correctly convert an invalid status to -01" in {
      val json = Json.parse("""{
                              | "code": "invalid status",
                              | "name": "invalid name"
        }""".stripMargin)
      val model = Json.fromJson[AwrsStatus](json)
      model.get.code mustBe "-01"
    }
  }

}