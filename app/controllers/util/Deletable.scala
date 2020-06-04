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

package controllers.util

import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import forms.AWRSEnums.BooleanRadioEnum
import forms.DeleteConfirmationForm._
import models.DeleteConfirmation
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.view_application.helpers.EditSectionOnlyMode

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}


trait Deletable[C, T] extends FrontendController with AwrsController {

  val applicationConfig: ApplicationConfig
  val mcc: MessagesControllerComponents
  implicit val ec: ExecutionContext = mcc.executionContext

  def fetch(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[C]]

  def save(authRetrievals: StandardAuthRetrievals, data: C)(implicit hc: HeaderCarrier): Future[C]

  val listToListObj: ListToListObj[T, C]
  val listObjToList: ListObjToList[C, T]
  val backCall: Call
  val section: String
  val deleteHeadingParameter: String
  val deleteFormAction: Int => Call
  val addNoAnswerRecord: List[T] => List[T]
  val amendHaveAnotherAnswer: AmendHaveAnotherAnswer[T]

  private final lazy val backAction: Future[Result] = Future.successful(Redirect(backCall))

  def fetchEntry(authRetrievals: StandardAuthRetrievals, id: Int)(implicit hc: HeaderCarrier): Future[T] =
    fetch(authRetrievals) map {
      case Some(list) => listObjToList(list)(id - 1)
      case _ => throw new RuntimeException("Unexpected error")
    }

  def showDeletePage(status: Deletable.this.Status)(form: Form[DeleteConfirmation], id: Int, model: T)(implicit request: Request[AnyContent], messages: Messages): Future[Result] =
    Future.successful(status(views.html.view_application.subviews.subview_delete_confirmation(form, section, deleteHeadingParameter, deleteFormAction, id, model)(request, messages = messages, applicationConfig = applicationConfig)) addLocation)

  def showDelete(id: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        fetchEntry(ar, id) flatMap (data =>
          showDeletePage(Ok)(deleteConfirmationForm, id, data))
      }
    }
  }

  def actionDelete(id: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        deleteConfirmationForm.bindFromRequest.fold(
          formWithErrors =>
            fetchEntry(ar, id) flatMap (data =>
              showDeletePage(BadRequest)(formWithErrors, id, data))
          ,
          confirmation =>
            confirmation.deleteConfirmation match {
              case Some(BooleanRadioEnum.YesString) =>
                actionDeleteCore(ar, id) flatMap (_ => backAction)
              case _ => backAction
            }
        )
      }
    }
  }

  // update the list if it is now empty to contain the default 'No' record if required for that section
  def updateIfEmpty(list: List[T]): List[T] = if (list.isEmpty) {
    addNoAnswerRecord(list)
  } else {
    list
  }

  //main delete
  // data (type)
  // id

  def actionDeleteCore(authRetrievals: StandardAuthRetrievals, id: Int)(implicit hc: HeaderCarrier): Future[C] = {
    lazy val aliasSanitise = (list: List[T]) => sanitise(list)(amendHaveAnotherAnswer, BooleanRadioEnum.YesString, BooleanRadioEnum.NoString)(EditSectionOnlyMode)
    require(id > 0)
    fetch(authRetrievals) flatMap {
      case Some(data) =>
        val list = removeElement(listObjToList(data), id)
        val treatedData = listToListObj(aliasSanitise(updateIfEmpty(list)))
        save(authRetrievals, treatedData)
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
