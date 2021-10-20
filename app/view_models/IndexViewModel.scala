/*
 * Copyright 2021 HM Revenue & Customs
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

package view_models

import utils.SessionSectionHashUtil

sealed trait IndexStatus {
  val messagesKey: String
  val cssClass: String

  final override def toString: String = messagesKey
}

case object SectionComplete extends IndexStatus {
  val messagesKey: String = "awrs.index_page.complete"
  val cssClass: String = "govuk-tag govuk-tag--turquoise"
}

case object SectionIncomplete extends IndexStatus {
  val messagesKey: String = "awrs.index_page.incomplete"
  val cssClass: String = "govuk-tag govuk-tag--yellow"
}

case object SectionEdited extends IndexStatus {
  val messagesKey: String = "awrs.index_page.edited"
  val cssClass: String = "govuk-tag govuk-tag--purple"
}

case object SectionNotStarted extends IndexStatus {
  val messagesKey: String = "awrs.index_page.not_started"
  val cssClass: String = "govuk-tag govuk-tag--grey"
}

case class IndexViewModel(sectionModels: List[SectionModel]) {
  // convert section models into boolean flags
  private val isCompleted: Seq[Boolean] = sectionModels.map {
    _.status match {
      case SectionComplete | SectionEdited => true
      case _ => false
    }
  }

  val toSessionHash: String = SessionSectionHashUtil.toHash(isCompleted)
}

case class SectionModel(id: String,
                        href: String,
                        text: String,
                        status: IndexStatus,
                        size: Option[Int] = None)
