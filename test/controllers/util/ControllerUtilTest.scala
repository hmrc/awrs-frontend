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

package controllers.util

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.ControllerUtil
import controllers.util.html.test_util_template
import forms.AWRSEnums.BooleanRadioEnum
import models.{Address, BCAddress}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.mvc.Results
import play.api.test.Helpers._
import utils.{AwrsUnitTestTraits, TestUtil}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class DummyData(doYouHaveAnyEntries: String, data: Option[String], doYouHaveAnotherEntry: Option[String])

class ControllerUtilTest extends AwrsUnitTestTraits with MockAuthConnector {

  val data = "data"

  val noentry = DummyData(BooleanRadioEnum.NoString, None, None)
  //
  val entry = DummyData(BooleanRadioEnum.YesString, data, BooleanRadioEnum.YesString)
  // the final entry of the list after sanitisation
  val lastEntry = DummyData(BooleanRadioEnum.YesString, data, BooleanRadioEnum.NoString)

  val haveAnotherAnswer = (data: DummyData) => data.doYouHaveAnotherEntry.get
  val hasSingleNoAnswer = (data: List[DummyData]) => data match {
    case Nil => ""
    case h :: _ => h.doYouHaveAnyEntries
  }
  val amendHaveAnotherAnswer = (data: DummyData, newAnswer: String) => data.copy(doYouHaveAnotherEntry = newAnswer)

  val allModes = List(LinearViewMode, EditSectionOnlyMode)

  "lookup" must {
    val newEntry = (id: Int) => Future.successful(Results.Ok(test_util_template(id, None)))
    val existingEntryAction = (list: List[DummyData], id: Int) => Future.successful(Results.Ok(test_util_template(id, list)))
    val haveAnother = (list: List[DummyData]) => list.last.doYouHaveAnotherEntry.contains(BooleanRadioEnum.YesString)
    val someDummyData = Future.successful(Some(List(lastEntry)))

    def testLookup(
                    fetchData: FetchData[List[DummyData]],
                    id: Int,
                    maxEntries: Option[Int] = None
                  )(
                    viewMode: ViewApplicationType
                  ) =
      lookup[List[DummyData], DummyData](
        fetchData = fetchData,
        id = id,
        (dummyData: List[DummyData]) => dummyData,
        maxEntries = maxEntries
      )(
        newEntry,
        existingEntryAction,
        haveAnother
      )(
        SessionBuilder.buildRequestWithSessionNoUser(),
        viewMode,
        messages,
        mockAppConfig,
        implicitly
      )

    "display a new page with id = 1 when no data is cached and id = 1 is requested" in {
      allModes.foreach { viewMode =>
        val showPage = testLookup(
          fetchData = Future.successful(None),
          id = 1
        )(
          viewMode
        )
        val document = Jsoup.parse(contentAsString(showPage))
        document.getElementById("idDiv").text mustBe "1"
        document.getElementById("dummyListHead") mustBe null
      }
    }

    "display a new page with id = 1 and populated when there is data cached and id = 1 is requested" in {
      allModes.foreach { viewMode =>
        val showPage = testLookup(
          fetchData = someDummyData,
          id = 1
        )(
          viewMode
        )
        val document = Jsoup.parse(contentAsString(showPage))
        document.getElementById("idDiv").text mustBe "1"
        document.getElementById("dummyListHead").text mustBe "data exists"
      }
    }

    "in linear viewMode, display page not found when id = 1 and populated and have another is answered no and id = 2 is requested" in {
      val showPage = testLookup(
        fetchData = someDummyData,
        id = 2
      )(
        LinearViewMode
      )
      status(showPage) mustBe NOT_FOUND
    }

    "in edit viewMode, display a new page when id = 1 and populated and have another is answered no and id = 2 is requested" in {
      val showPage = testLookup(
        fetchData = someDummyData,
        id = 2
      )(
        EditSectionOnlyMode
      )
      status(showPage) mustBe OK
      val document = Jsoup.parse(contentAsString(showPage))
      document.getElementById("idDiv").text mustBe "2"
      document.getElementById("dummyListHead") mustBe null
    }

    "display page not found when no data is cached and id = 2 is requested" in {
      allModes.foreach { viewMode =>
        val showPage = testLookup(
          fetchData = Future.successful(None),
          id = 2
        )(
          viewMode
        )
        status(showPage) mustBe NOT_FOUND
      }
    }

    "display page not found when 1 data entry is cached and id = 3 is requested" in {
      allModes.foreach { viewMode =>
        val showPage = testLookup(
          fetchData = someDummyData,
          id = 3
        )(
          viewMode
        )
        status(showPage) mustBe NOT_FOUND
      }
    }

    "display page not found when 1 data entry is cached and id = 2 is requested but the upper limit is set to 1" in {
      allModes.foreach { viewMode =>
        val showPage = testLookup(
          fetchData = someDummyData,
          id = 2,
          maxEntries = 1
        )(
          viewMode
        )
        status(showPage) mustBe NOT_FOUND
      }
    }
  }


  "sanitise" must {
    val amendHaveAnotherAnswer = (data: DummyData, newAnswer: String) => data.copy(doYouHaveAnotherEntry = newAnswer)

    "all elements in the middle of the list with have another answered as no will be set to yes, in linear viewMode the answer to last element must not be amended" in {
      val list = List(lastEntry, entry) // have anothers must be (no , yes)
      val result = sanitise[DummyData](list = list)(
        amendHaveAnotherAnswer = amendHaveAnotherAnswer,
        yes = BooleanRadioEnum.YesString,
        no = BooleanRadioEnum.NoString
      )(
        LinearViewMode
      )
      result mustBe List(entry, entry) // expected have another must be (yes, yes)
    }

    "all elements in the middle of the list with have another answered as no will be set to yes, in edit viewMode the answer to the last element will always have have another set to no" in {
      val list = List(lastEntry, lastEntry)
      val result = sanitise[DummyData](list = list)(
        amendHaveAnotherAnswer = amendHaveAnotherAnswer,
        yes = BooleanRadioEnum.YesString,
        no = BooleanRadioEnum.NoString
      )(
        EditSectionOnlyMode
      )
      result mustBe List(entry, lastEntry)

      val list2 = List(lastEntry, entry)
      val result2 = sanitise[DummyData](list = list2)(
        amendHaveAnotherAnswer = amendHaveAnotherAnswer,
        yes = BooleanRadioEnum.YesString,
        no = BooleanRadioEnum.NoString
      )(
        EditSectionOnlyMode
      )
      result2 mustBe List(entry, lastEntry)
    }

  }

  "updateList" must {

    object TestControllerUtil extends ControllerUtil {
      def testUpdateList(list: List[DummyData],
                         id: Int,
                         data: DummyData,
                         viewMode: ViewApplicationType
                        ): Option[List[DummyData]] =
        updateList(list, id, data, viewMode)(
          haveAnotherAnswer,
          amendHaveAnotherAnswer,
          hasSingleNoAnswer(list).eq(BooleanRadioEnum.NoString),
          BooleanRadioEnum.YesString,
          BooleanRadioEnum.NoString)

    }

    "if the fetched data was there are no entries, then the element list is not updated but rather replaced" in {
      val list = List(noentry)
      val id = 1
      val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.NoString)
      val viewMode = LinearViewMode

      val updated = TestControllerUtil.testUpdateList(
        list, id, newData, viewMode
      )

      updated.get mustBe List(newData)
    }

    "update the entries correctly in linear viewMode" must {
      // in linear viewMode the answer to have another determines if the list can potentially be trimmed

      "when have another is set to no and id is not the last element in the list, update the elemnet and removes the rest" in {
        val list = List(entry, entry, lastEntry)
        val id = 1
        val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.NoString)
        val viewMode = LinearViewMode

        val updated = TestControllerUtil.testUpdateList(
          list, id, newData, viewMode
        )

        updated.get mustBe list.updated(id - 1, newData).take(id)
      }

      "when have another is set to yes, update the elemnt and do not edit the rest" in {
        val list = List(entry, entry, entry)
        val id = 1
        val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.YesString)
        val viewMode = LinearViewMode

        val updated = TestControllerUtil.testUpdateList(
          list, id, newData, viewMode
        )

        updated.get mustBe list.updated(id - 1, newData)
      }
    }

    "update the entries correctly in edit viewMode" in {
      val list = List(entry, entry, entry) // note the last element indicates there is another to be added
      val id = 1
      val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.NoString) // note this indicates another won't be added
      val viewMode = EditSectionOnlyMode

      val updated = TestControllerUtil.testUpdateList(
        list, id, newData, viewMode
      )

      // in edit viewMode the list will be sanitised
      // in the expectation the add another for id 1 is updated to yes since there will be another after this entry
      // the final entry is also updated to indicate no additional entries are expected
      updated.get mustBe list.updated(id - 1, newData.copy(doYouHaveAnotherEntry = BooleanRadioEnum.YesString)).updated(list.size - 1, lastEntry)
    }

    "id from 1 more than max will be appended" in {
      val list = List(entry, lastEntry)
      val id = list.size + 1

      allModes.foreach { viewMode =>
        val updated = TestControllerUtil.testUpdateList(
          list, id, entry, viewMode
        )

        viewMode match {
          case LinearViewMode => updated.get mustBe List(entry, entry, entry) // must not have updated the last element
          case EditSectionOnlyMode => updated.get mustBe List(entry, entry, lastEntry) // must have updated the last element
          case _ => throw new RuntimeException("unexpected viewMode in test")
        }
      }

    }

    "id from more than 1 more than max will result in None being returned" in {
      val list = List(lastEntry)
      val id = list.size + 2
      val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.NoString)

      allModes.foreach { viewMode =>
        val updated = TestControllerUtil.testUpdateList(
          list, id, newData, viewMode
        )

        updated mustBe None
      }

    }
  }

  "saveThenRedirect" must {
    val redirect = (_: String, id: Int) => Future.successful(Results.Ok(test_util_template(id, None)))
    val fetch = (data: Option[List[DummyData]]) => Future.successful(data)
    val save: SaveData[List[DummyData]] = (_, data) => Future.successful(data)
    val conv = (list: List[DummyData]) => list


    val mockSave = mock[SaveData[List[DummyData]]]

    def mockSaveAction(testFunction: List[DummyData] => Unit): Unit = {
      reset(mockSave)
      val mockSaveAndTestItsInput: Answer[Future[Option[Any]]] = (invocation: InvocationOnMock) => {
        val dummyDataArg: List[DummyData] = {
          invocation.getArguments.find {
            case _: List[_] => true
            case _ => false
          }.map(_.asInstanceOf[List[DummyData]]).getOrElse(List.empty[DummyData])
        }
        testFunction(dummyDataArg)
        Future.successful(Some(dummyDataArg))
      }
      when(mockSave(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(mockSaveAndTestItsInput)
    }

    def testSaveThenRedirect(
                              fetchData: FetchData[List[DummyData]],
                              saveData: SaveData[List[DummyData]],
                              id: Int,
                              data: DummyData
                            )(
                              viewMode: ViewApplicationType
                            ) =
      saveThenRedirect[List[DummyData], DummyData](
        fetchData,
        saveData,
        id,
        data,
        TestUtil.defaultAuthRetrieval
      )(
        haveAnotherAnswer = haveAnotherAnswer,
        amendHaveAnotherAnswer = amendHaveAnotherAnswer,
        hasSingleNoAnswer = hasSingleNoAnswer
      )(
        listObjToList = conv,
        listToListObj = conv
      )(
        redirectRoute = redirect
      )(
        SessionBuilder.buildRequestWithSessionNoUser(),
        viewMode,
        messages,
        mockAppConfig,
        implicitly
      )


    "if fetch returned no data and the save id = 1 then persist the new data in a new list" in {
      val returnedData = None
      val id = 1

      allModes.foreach { viewMode =>
        mockSaveAction(
          (list: List[DummyData]) => {
            list mustBe List(entry)
          }
        )
        val result = testSaveThenRedirect(
          fetchData = fetch(returnedData),
          saveData = mockSave,
          id = id,
          data = entry
        )(
          viewMode
        )
        status(result) mustBe OK
      }
    }

    "if fetch returned no data and the save id is not 1 then show error page" in {
      val returnedData = None
      val id = 2

      allModes.foreach { viewMode =>
        val result = testSaveThenRedirect(fetch(returnedData),
          saveData = save,
          id = id,
          data = entry
        )(
          viewMode
        )
        status(result) mustBe BAD_REQUEST
      }
    }

    "if fetch returned data but the requested id is 1 element beyond the returend list range then append the data to the end of the list" in {
      val returnedData = List(entry, lastEntry)
      val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.YesString)

      val id = returnedData.size + 1

      allModes.foreach { viewMode =>
        mockSaveAction(
          (list: List[DummyData]) => {
            list.size mustBe returnedData.size + 1
            val modeDependedAppendedData = viewMode match {
              case LinearViewMode => newData
              // in edit section viewMode the final entry is always amended to set the anser to have another to no
              case EditSectionOnlyMode => newData.copy(doYouHaveAnotherEntry = BooleanRadioEnum.NoString)
              case _ => throw new RuntimeException("unexpected viewMode in test")
            }
            list mustBe returnedData.updated(returnedData.size - 1, returnedData.last.copy(doYouHaveAnotherEntry = BooleanRadioEnum.YesString)) :+ modeDependedAppendedData
          }
        )
        val result = testSaveThenRedirect(
          fetchData = fetch(returnedData),
          saveData = save,
          id = id,
          data = newData
        )(
          viewMode
        )
        status(result) mustBe OK
      }
    }

    "if fetch returned data but the requested id is out of the expected range then show error page" in {
      val returnedData = List(lastEntry)
      // returnedData.size + 1 must trigger append instead
      val id = returnedData.size + 2
      allModes.foreach { viewMode =>
        val result = testSaveThenRedirect(
          fetchData = fetch(returnedData),
          saveData = save,
          id = id,
          data = entry
        )(
          viewMode
        )
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "if fetch returned some data then update the item specified without changing the other entries" in {
      val returnedData = List(entry, entry, lastEntry) // populated list, content doesn't matter as long as doYouHaveAnyEntries in the head position is not set to no
      val id = 1
      val newData = DummyData(BooleanRadioEnum.YesString, "updated data", BooleanRadioEnum.YesString)

      allModes.foreach { viewMode =>
        mockSaveAction(
          (list: List[DummyData]) => {
            list mustBe returnedData.updated(id - 1, newData)
            list diff returnedData mustBe List(newData)
          }
        )
        val result = testSaveThenRedirect(
          fetchData = fetch(returnedData),
          saveData = mockSave,
          id = id,
          data = newData
        )(
          viewMode
        )
        status(result) mustBe OK
      }

    }
  }

  "convertBCAddressToAddress" in {
    val addl1 = "address Line1"
    val addl2 = "address Line2"
    val addl3 = "address Line3"
    val addl4 = "address Line4"
    val pc = "NE28 8ER"
    val country = "GB"

    def bcAddress = {
      val add = BCAddress(addl1, addl2, Some(addl3), Some(addl4), Some(pc), country)
      convertBCAddressToAddress(add).get
    }
    val expectedAddress = Address(addl1, addl2, Some(addl3), Some(addl4), Some(pc),None,country)
    bcAddress mustBe expectedAddress
  }

}
