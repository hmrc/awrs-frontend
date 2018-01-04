/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.auth.AwrsController
import forms.AWRSEnums.BooleanRadioEnum
import forms.DeleteConfirmationForm._
import models.DeleteConfirmation
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import views.view_application.helpers.EditSectionOnlyMode

import scala.annotation.tailrec
import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.http.HeaderCarrier

trait Deletable[C, T] extends AwrsController {

  //fetch function
  //save function
  //back
  def fetch(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[C]]

  def save(data: C)(implicit user: AuthContext, hc: HeaderCarrier): Future[C]

  val listToListObj: ListToListObj[T, C]
  val listObjToList: ListObjToList[C, T]
  val backCall: Call
  val section: String
  val deleteHeadingParameter: String
  val deleteFormAction: Int => Call
  val addNoAnswerRecord: List[T] => List[T]
  val amendHaveAnotherAnswer: AmendHaveAnotherAnswer[T]

  private final lazy val backAction: Future[Result] = Future.successful(Redirect(backCall))

  def fetchEntry(id: Int)(implicit user: AuthContext, hc: HeaderCarrier): Future[T] =
    fetch map {
      case Some(list) => listObjToList(list)(id - 1)
      case _ => throw new RuntimeException("Unexpected error")
    }

  def showDeletePage(status: Deletable.this.Status)(form: Form[DeleteConfirmation], id: Int, model: T)(implicit request: Request[AnyContent]) =
    Future.successful(status(views.html.view_application.subviews.subview_delete_confirmation(form, section, deleteHeadingParameter, deleteFormAction, id, model)) addLocation)

  //show
  // id
  def showDelete(id: Int) = asyncRestrictedAccess {
    implicit user => implicit request =>
      fetchEntry(id) flatMap {
        case data =>
          showDeletePage(Ok)(deleteConfirmationForm, id, data)
      }
  }

  def actionDelete(id: Int) = asyncRestrictedAccess {
    implicit user => implicit request =>
      deleteConfirmationForm.bindFromRequest.fold(
        formWithErrors =>
          fetchEntry(id) flatMap {
            case data =>
              showDeletePage(BadRequest)(formWithErrors, id, data)
          }
        ,
        confirmation =>
          confirmation.deleteConfirmation match {
            case Some(BooleanRadioEnum.YesString) =>
              actionDeleteCore(id) flatMap {
                case _ => backAction
              }
            case _ => backAction
          }
      )
  }

  // update the list if it is now empty to contain the default 'No' record if required for that section
  def updateIfEmpty(list: List[T]): List[T] = list.isEmpty match {
    case true => addNoAnswerRecord(list)
    case false => list
  }

  //main delete
  // data (type)
  // id

  def actionDeleteCore(id: Int)(implicit user: AuthContext, hc: HeaderCarrier): Future[C] = {
    lazy val aliasSanitise = (list: List[T]) => sanitise(list)(amendHaveAnotherAnswer, BooleanRadioEnum.YesString, BooleanRadioEnum.NoString)(EditSectionOnlyMode)
    require(id > 0)
    fetch flatMap {
      case Some(data) =>
        val list = removeElement(listObjToList(data), id)
        val treatedData = listToListObj(aliasSanitise(updateIfEmpty(list)))
        save(treatedData)
      case None =>
        Future.failed(throw new RuntimeException("unexpected error"))
    }
  }

  // list delete
  // list
  // id
  def removeElement(list: List[T], id: Int): List[T] = {
    require(id > 0 && id <= list.size)
    @tailrec
    def main(list: List[T], index: Int, out: List[T]): List[T] = list match {
      case h :: t if index != id => main(t, index + 1, out :+ h)
      case h :: t if index == id => main(t, index + 1, out)
      case Nil => out
    }
    main(list, 1, List())
  }
}
