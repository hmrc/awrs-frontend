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

package controllers

import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import forms.AWRSEnums.BooleanRadioEnum
import models.{Address, BCAddress}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}


private[controllers] trait ControllerUtil {

  // spec consistency notes:
  // C is defined for the container and T is defined for the underlying element
  // A (any) can be defined for either C or T
  type ToList[C, T] = (C) => List[T]
  type NewEntryAction = (Int) => Future[Result]
  type ExistingEntryAction[C] = (C, Int) => Future[Result]
  type HaveAnother[A] = (A) => Boolean
  type FetchData[C] = Future[Option[C]]

  private val min = 1 // minimum id for any list based object must be 1

  /*
  * this function is designed to aid views that needs to display an entry in a list based object
  * it checks the range of the requested id to determine if the user can view the requested the page or it is out of range
  */
  def lookup[C, T](fetchData: FetchData[C], // call to fetch the data from save4later
                   id: Int, // the id of the entry to be displayed (can be a new entry)
                   toList: ToList[C, T], // conversion function from the input data type to a List
                   maxEntries: Option[Int] = None // optional max entries in the list
                  )(newEntryAction: NewEntryAction,
                    existingEntryAction: ExistingEntryAction[C],
                    haveAnother: HaveAnother[C]
                  )(implicit request: Request[AnyContent],
                    viewMode: ViewApplicationType,
                    messages: Messages,
                    applicationConfig: ApplicationConfig,
                    ec: ExecutionContext
                  ): Future[Result] =
  fetchData flatMap {
    case Some(data) =>
      val total = toList(data).size
      // if maxEntries is defined the the upper bound of the range validation is either the capped by the size of
      // the list or the maxEntries itself
      lazy val withinMaxLim = maxEntries match {
        case Some(lim) => id <= lim
        case None => true
      }
      // the isIncomplete function is only applicable for LinearViewMode
      // after the first journey the user need to be able to add another entry via the view-section pages
      // in these modes viewMode would not be linearViewMode and we no longer care about what the user
      // answered in their previous question
      val overrideIsIncomplete = viewMode match {
        case LinearViewMode => haveAnother(data)
        case _ => true
      }
      id match {
        // if the id is for a new entry (when the id is just outside the range of the list but the user indicated a
        // further entry but within the max range (if defined))
        case _ if (id == total + 1 && withinMaxLim) && overrideIsIncomplete => newEntryAction(id)
        // if the id is for an existing entry (when the id is within the range of the list)
        case _ if id >= min && id <= total => existingEntryAction(data, id)
        // if id is out of range
        case _ => AwrsController.showNotFoundPage(applicationConfig.templateNotFound)
      }
    case None =>
      // if nothing is returned then this is either for the very first entry or there was an error
      id match {
        // this needs to be after the fetch call because it could be an amendment to the first entry
        case 1 => newEntryAction(1)
        case _ => AwrsController.showNotFoundPage(applicationConfig.templateNotFound)
      }
  }

  // function used to fix the answers to do you have another _ in the list
  // every element except the last one will be set to yes and the last one will be set to no
  def sanitise[T](
                   list: List[T])(
                   amendHaveAnotherAnswer: AmendHaveAnotherAnswer[T],
                   yes: String,
                   no: String
                 )(viewMode: ViewApplicationType): List[T] = {
    @tailrec
    def main(init: List[T], out: List[T]): List[T] = {
      init match {
        case h :: Nil => out :+ {
          viewMode match {
            case LinearViewMode => h // do not edit the answer to the final question in linear view mode
            case _ => amendHaveAnotherAnswer(h, no)
          }
        }
        case h :: t => main(t, out :+ amendHaveAnotherAnswer(h, yes))
        case Nil => Nil
      }
    }
    main(list, List[T]())
  }

  protected def updateList[T](list: List[T],
                              id: Int,
                              data: T,
                              viewMode: ViewApplicationType
                             )(
                               haveAnotherAnswer: HaveAnotherAnswer[T],
                               amendHaveAnotherAnswer: AmendHaveAnotherAnswer[T],
                               hasSingleNoAnswerResult: Boolean,
                               yes: String,
                               no: String
                             ): Option[List[T]] = {
    val listSize = list.size
    // if the only existing entry is a record stating that no records exist, remove it and add the new record...
    if (listSize == 1 && hasSingleNoAnswerResult) {
      Some(List() :+ data)
    } else {
      lazy val aliasSanitise = (list: List[T]) => sanitise(list)(amendHaveAnotherAnswer, yes, no)(viewMode)
      id match {
        case _ if listSize + 1 == id => Some(aliasSanitise(list :+ data))
        case _ if id >= 1 && id <= listSize =>
          val updatedList = list.updated(id - 1, data)
          viewMode match {
            case LinearViewMode =>
              val trim = haveAnotherAnswer(data).equals(no)
              Some(
                if (trim) {
                  updatedList.take(id)
                } else {
                  updatedList
                }
              )
            case EditSectionOnlyMode =>
              Some(aliasSanitise(updatedList))
            case _ => None
          }
        case _ => None
      }
    }
  }

  type RedirectRoute = (String, Int) => Future[Result]
  type SaveData[C] = (StandardAuthRetrievals, C) => Future[C]
  type HaveAnotherAnswer[C] = (C) => String
  type AmendHaveAnotherAnswer[T] = (T, String) => T
  type HasSingleNoAnswer[C] = (C) => String

  type ListObjToList[C, T] = C => List[T]
  type ListToListObj[T, C] = List[T] => C

  def saveThenRedirect[C, T](fetchData: FetchData[C],
                             saveData: SaveData[C],
                             id: Int,
                             data: T,
                             authRetrievals: StandardAuthRetrievals
                            )(
                              haveAnotherAnswer: HaveAnotherAnswer[T],
                              amendHaveAnotherAnswer: AmendHaveAnotherAnswer[T],
                              // TODO give this more meaningful name
                              hasSingleNoAnswer: HasSingleNoAnswer[C],
                              yes: String = BooleanRadioEnum.YesString,
                              no: String = BooleanRadioEnum.NoString
                            )(
                              listObjToList: ListObjToList[C, T],
                              listToListObj: ListToListObj[T, C]
                            )(
                              redirectRoute: RedirectRoute
                            )(
                              implicit request: Request[AnyContent],
                              viewMode: ViewApplicationType,
                              messages: Messages,
                              applicationConfig: ApplicationConfig,
                              ec: ExecutionContext
                            ): Future[Result] = {
    lazy val toNextPage = (data: T) => redirectRoute(haveAnotherAnswer(data), id)
    // set redirect based on un-cleansed data so that it follows the route based on the addAnother question before it is reset to 'No'
    val redirectTo = toNextPage(data)
    fetchData flatMap {
      case Some(listObj) =>
        updateList(listObjToList(listObj), id, data, viewMode)(haveAnotherAnswer, amendHaveAnotherAnswer, hasSingleNoAnswer(listObj).equals(no), yes, no) match {
          case Some(updated) => saveData(authRetrievals, listToListObj(updated)) flatMap (_ => redirectTo)
          case _ => AwrsController.showErrorPage(applicationConfig.templateAppError)
        }
      case None if id == 1 =>
        saveData(authRetrievals, listToListObj(List(data))) flatMap (_ => redirectTo)
      case None => AwrsController.showBadRequest(applicationConfig.templateAppError)
    }
  }

  def convertBCAddressToAddress(bcAdd: BCAddress): Option[Address] =
    Some(Address(addressLine1 = bcAdd.line_1,
      addressLine2 = bcAdd.line_2,
      addressLine3 = bcAdd.line_3,
      addressLine4 = bcAdd.line_4,
      postcode = bcAdd.postcode,
      addressCountry = None,
      addressCountryCode = bcAdd.country))

}

package object util extends ControllerUtil
