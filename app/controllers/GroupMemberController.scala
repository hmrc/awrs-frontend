/*
 * Copyright 2017 HM Revenue & Customs
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

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import forms.AWRSEnums.BooleanRadioEnum
import forms.GroupMemberDetailsForm._
import models.{GroupMember, GroupMembers}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Call, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}
import util._

import scala.concurrent.Future

trait GroupMemberController extends AwrsController with JourneyPage with AccountUtils with Deletable[GroupMembers, GroupMember] with SaveAndRoutable {

  //fetch function
  override def fetch(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[GroupMembers]] = save4LaterService.mainStore.fetchGroupMembers

  override def save(data: GroupMembers)(implicit user: AuthContext, hc: HeaderCarrier): Future[GroupMembers] = save4LaterService.mainStore.saveGroupMembers(data)

  override val listToListObj = (groupMemberDetails: List[GroupMember]) => GroupMembers(groupMemberDetails)
  override val listObjToList = (groupMemberDetails: GroupMembers) => groupMemberDetails.members
  override val backCall: Call = controllers.routes.ViewApplicationController.viewSection(groupMembersName)
  override val section: String = groupMembersName
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.GroupMemberController.actionDelete(id)
  override lazy val deleteHeadingParameter: String = Messages("awrs.view_application.group")
  override val addNoAnswerRecord: (List[GroupMember]) => List[GroupMember] = (emptyList) => emptyList
  override val amendHaveAnotherAnswer = (data: GroupMember, newAnswer: String) => data.copy(addAnotherGrpMember = Some(newAnswer))

  def showMemberDetails(id: Int, isLinearMode: Boolean, isNewRecord: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      lazy val newEntryAction = (id: Int) =>
        Future.successful(Ok(views.html.awrs_group_member(groupMemberForm.form, id, isNewRecord)))

      lazy val existingEntryAction = (data: GroupMembers, id: Int) =>
        Future.successful(Ok(views.html.awrs_group_member(groupMemberForm.form.fill(data.members(id - 1)), id, isNewRecord)))

      lazy val haveAnother = (data: GroupMembers) => {
        val list = data.members
        list.last.addAnotherGrpMember.fold("")(x => x).equals(BooleanRadioEnum.YesString)
      }

      lookup[GroupMembers, GroupMember](
        fetchData = fetch,
        id = id,
        toList = (members: GroupMembers) => members.members
      )(
        newEntryAction = newEntryAction,
        existingEntryAction = existingEntryAction,
        haveAnother = haveAnother
      )
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    groupMemberForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_group_member(formWithErrors, id, isNewRecord))),
      groupMemberData =>
        saveThenRedirect[GroupMembers, GroupMember](
          fetchData = fetch,
          saveData = save,
          id = id,
          data = groupMemberData
        )(
          haveAnotherAnswer = (data: GroupMember) => data.addAnotherGrpMember.fold("")(x => x),
          amendHaveAnotherAnswer = amendHaveAnotherAnswer,
          hasSingleNoAnswer = (fetchData: GroupMembers) => "This is not required for group members"
        )(
          listObjToList = (listObj: GroupMembers) => listObj.members,
          listToListObj = (list: List[GroupMember]) => GroupMembers(list)
        )(
          redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
        )
    )
  }

}

object GroupMemberController extends GroupMemberController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
