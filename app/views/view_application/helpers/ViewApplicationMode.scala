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

package views.view_application.helpers

import play.twirl.api.Html


sealed trait ViewApplicationType

case object LinearViewMode extends ViewApplicationType

case object OneViewMode extends ViewApplicationType

case object EditSectionOnlyMode extends ViewApplicationType

case object EditRecordOnlyMode extends ViewApplicationType

case object EditMode extends ViewApplicationType

case object PrintFriendlyMode extends ViewApplicationType

object SubViewTemplateHelper {

  val noContent = Html("")

  def dynamicContent(default: Option[Html] = None, ifOneViewMode: Option[Html] = None, ifEditMode: Option[Html] = None, ifPrintFriendlyMode: Option[Html] = None)(implicit viewType: ViewApplicationType): Html =
    viewType match {
      case OneViewMode if ifOneViewMode.isDefined => ifOneViewMode.get
      case EditSectionOnlyMode if ifEditMode.isDefined => ifEditMode.get
      case EditRecordOnlyMode if ifEditMode.isDefined => ifEditMode.get
      case EditMode if ifEditMode.isDefined => ifEditMode.get
      case PrintFriendlyMode if ifPrintFriendlyMode.isDefined => ifPrintFriendlyMode.get
      case _ => default match {
        case Some(html) => html
        case _ => noContent
      }
    }

  def dynamicContent(oneView: Html, edit: Html, printFriendly: Html)(implicit viewType: ViewApplicationType): Html =
    dynamicContent(None, Some(oneView), Some(edit), Some(printFriendly))

  def isEditMode()(implicit viewType: ViewApplicationType): Boolean =
    viewType match {
      case EditRecordOnlyMode | EditSectionOnlyMode | EditMode => true
      case _ => false
    }

  def isSectionEdit()(implicit viewType: ViewApplicationType): Boolean =
    viewType match {
      case EditSectionOnlyMode | EditMode => true
      case _ => false
    }

  def isRecordEdit()(implicit viewType: ViewApplicationType): Boolean =
    viewType match {
      case EditRecordOnlyMode | EditMode => true
      case _ => false
    }

  def isPrintFriendly()(implicit viewType: ViewApplicationType): Boolean =
    viewType match {
      case PrintFriendlyMode => true
      case _ => false
    }

  def isOneViewMode()(implicit viewType: ViewApplicationType): Boolean =
    viewType match {
      case OneViewMode => true
      case _ => false
    }

  def headingPrefix(enter: String = "awrs.generic.enter", edit: String = "awrs.generic.edit", isNewRecord: Boolean = false)(implicit viewType: ViewApplicationType): String =
    (isEditMode, isNewRecord) match {
      case (true, false) => edit
      case (_, _) => enter
    }

}
