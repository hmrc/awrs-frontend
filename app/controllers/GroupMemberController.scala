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

package controllers

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.StandardAuthRetrievals
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.GroupMemberDetailsForm._
import javax.inject.Inject
import models.{GroupMember, GroupMembers}
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class GroupMemberController @Inject()(val mcc: MessagesControllerComponents,
                                      val save4LaterService: Save4LaterService,
                                      val deEnrolService: DeEnrolService,
                                      val authConnector: DefaultAuthConnector,
                                      val auditable: Auditable,
                                      val accountUtils: AccountUtils,
                                      implicit val applicationConfig: ApplicationConfig,
                                      template: views.html.awrs_group_member) extends FrontendController(mcc) with JourneyPage with Deletable[GroupMembers, GroupMember] with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  //fetch function
   def fetch(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[GroupMembers]] = save4LaterService.mainStore.fetchGroupMembers(authRetrievals)

   def save(authRetrievals: StandardAuthRetrievals, data: GroupMembers)(implicit hc: HeaderCarrier): Future[GroupMembers] =
    save4LaterService.mainStore.saveGroupMembers(authRetrievals, data)

  override val listToListObj: List[GroupMember] => GroupMembers = (groupMemberDetails: List[GroupMember]) => GroupMembers(groupMemberDetails)
  override val listObjToList: GroupMembers => List[GroupMember] = (groupMemberDetails: GroupMembers) => groupMemberDetails.members
  override lazy val backCall: Call = controllers.routes.ViewApplicationController.viewSection(groupMembersName)
  override val section: String = groupMembersName
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.GroupMemberController.actionDelete(id)
  override lazy val deleteHeadingParameter: String = "awrs.view_application.group"
  override val addNoAnswerRecord: List[GroupMember] => List[GroupMember] = emptyList => emptyList
  override val amendHaveAnotherAnswer: (GroupMember, String) => GroupMember = (data: GroupMember, newAnswer: String) => data.copy(addAnotherGrpMember = Some(newAnswer))

  def showMemberDetails(id: Int, isLinearMode: Boolean, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        lazy val newEntryAction = (id: Int) =>
          Future.successful(Ok(template(groupMemberForm.form, id, isNewRecord, ar.enrolments, accountUtils)))

        lazy val existingEntryAction = (data: GroupMembers, id: Int) =>
          Future.successful(Ok(template(groupMemberForm.form.fill(data.members(id - 1)), id, isNewRecord, ar.enrolments, accountUtils)))

        lazy val haveAnother = (data: GroupMembers) => {
          val list = data.members
          list.last.addAnotherGrpMember.fold("")(x => x).equals(BooleanRadioEnum.YesString)
        }

        lookup[GroupMembers, GroupMember](
          fetchData = fetch(ar),
          id = id,
          toList = (members: GroupMembers) => members.members
        )(
          newEntryAction = newEntryAction,
          existingEntryAction = existingEntryAction,
          haveAnother = haveAnother
        )
      }
    }
  }

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, ar: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    groupMemberForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, id, isNewRecord, ar.enrolments, accountUtils))),
      groupMemberData =>
        saveThenRedirect[GroupMembers, GroupMember](
          fetchData = fetch(ar),
          saveData = save,
          id = id,
          data = groupMemberData,
          authRetrievals = ar
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