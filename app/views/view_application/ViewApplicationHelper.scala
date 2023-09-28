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

package views.view_application

import services.DataCacheKeys._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.CacheUtil
import views.view_application.helpers.SubViewTemplateHelper._
import views.view_application.helpers.ViewApplicationType

object ViewApplicationHelper {

  val NoneBreakingSpace = "\u00A0"

  implicit val cacheUtil: CacheMap => CacheUtil.CacheHelper = CacheUtil.cacheUtil

  implicit class OptionStringUtil(someStr: Option[String]) {
    def `+`(anotherSomeStr: Option[String]): Option[String] = (someStr, anotherSomeStr) match {
      case (None, None) => None
      case _ =>
        Some(someStr.fold("")(x => x) + anotherSomeStr.fold("")(x => x))
    }
  }

  implicit class StringUtil(str: String) {
    // This class is created because:
    // " " + Some(string) will return " Some(string)"
    // This is due to + being defined in the String class itself and hence will take precedence over any definitions
    // written here.
    // the x symbol function is defined for the cases where the desired concatation needs to be led by a string
    def `x`(anotherSomeStr: Option[String]): Option[String] = anotherSomeStr match {
      case None => Some(str)
      case _    => Some(str + anotherSomeStr.fold("")(x => x))
    }
  }

  def countContent(rows: Iterable[Option[String]]): Int =
    rows.foldLeft(0)(
      (count, r) =>
        count + (r match {
          case Some(str) if !str.equals("") => 1
          case _ => 0
        }))

  def link(href: Option[String], message: String, classAttr: String, idAttr: Option[String] = None, visuallyHidden: String = ""): String = {
    "<a " + {
      val ida = idAttr match {
        case Some(id) => "id=\"" + id + "\""
        case _ => ""
      }
      ida
    } + " class=\"" + classAttr + "\" href=\"" + href.get + "\">" + message + {
      visuallyHidden match {
        case "" => ""
        case text => "<span class=\"govuk-visually-hidden\">" + text + "</span>"
      }
    } + "</a>"
  }

  def edit_link(editUrl: Int => String, id: Int, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType): String = {

    if (isSectionEdit()) {
      link(
        Some(editUrl(id)),
        "Edit",
        classAttr = "govuk-link govuk-!-padding-left-9",
        idAttr = Some("edit-" + id),
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  def edit_link_s(editUrl: String, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType): String = {

    if (isRecordEdit()) {
      link(
        Some(editUrl),
        "Edit",
        classAttr = "govuk-link",
        idAttr = Some("edit-link"),
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  def edit_link_sl(editUrl: String, idx: String, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType): String = {

    if (isRecordEdit()) {
      link(
        Some(editUrl),
        "Edit",
        classAttr = "govuk-link",
        idAttr = Some(s"edit-link-$idx"),
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }


  def delete_link(deleteUrl: Int => String, id: Int, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType): String = {

    if (isSectionEdit()) {
      link(
        Some(deleteUrl(id)),
        "Delete",
        classAttr = "govuk-link govuk-!-padding-left-3",
        idAttr = Some("delete-" + id),
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  def getSectionDisplayName(sectionName: String, legalEntity: String): String = {
    (sectionName, legalEntity) match {
      case (`businessDetailsName`, "Partnership" | "LP" | "LLP") => "awrs.view_application.partnership_details_text"
      case (`businessDetailsName`, "LLP_GRP" | "LTD_GRP") => "awrs.view_application.group_business_details_text"
      case (`businessDetailsName`, _) => "awrs.view_application.business_details_text"
      case (`businessRegistrationDetailsName`, "Partnership" | "LP" | "LLP") => "awrs.view_application.partnership_registration_details_text"
      case (`businessRegistrationDetailsName`, "LLP_GRP" | "LTD_GRP") => "awrs.view_application.group_business_registration_details_text"
      case (`businessRegistrationDetailsName`, _) => "awrs.view_application.business_registration_details_text"
      case (`placeOfBusinessName`, "Partnership" | "LP" | "LLP") => "awrs.view_application.partnership_place_of_business_text"
      case (`placeOfBusinessName`, "LLP_GRP" | "LTD_GRP") => "awrs.view_application.group_place_of_business_text"
      case (`placeOfBusinessName`, _) => "awrs.view_application.place_of_business_text"
      case (`businessContactsName`, "Partnership" | "LP" | "LLP") => "awrs.view_application.partnership_contacts_text"
      case (`businessContactsName`, "LLP_GRP" | "LTD_GRP") => "awrs.view_application.group_business_contacts_text"
      case (`businessContactsName`, _) => "awrs.view_application.business_contacts_text"
      case (`partnersName`, _) => "awrs.view_application.business_partners_text"
      case (`groupMembersName`, _) => "awrs.view_application.group_member_details_text"
      case (`additionalBusinessPremisesName`, _) => "awrs.view_application.additional_premises_text"
      case (`businessDirectorsName`, _) => "awrs.view_application.business_directors.index_text"
      case (`tradingActivityName`, _) => "awrs.view_application.trading_activity_text"
      case (`productsName`, _) => "awrs.view_application.products_text"
      case (`suppliersName`, _) => "awrs.view_application.suppliers_text"
      case (`applicationDeclarationName`, _) => "awrs.view_application.application_declaration_text"
      case _ => ""
    }
  }

}
