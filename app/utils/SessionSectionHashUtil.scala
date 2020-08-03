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

package utils

/**
  * This class is created to address the issue in AWRS-1594 to determine if save and continue during the linear journey
  * must goto the next section or return to index
  *
  * The problem:
  * When the user begins the linear journey they can fill out the forms in any order they like. If they have filled in
  * a section then when they visit the section again they must be brought to the edit view instead of continuing
  * in the linear journey. However if the next section is something they have already completed then they are expected
  * to be taken to the index page instead.
  *
  * The current solution:
  * A hash of the section status is updated by the index controller every time it is visited, this describes which of
  * the sections are completed at this point in time. When save and continue is called, the respective controller then
  * checks this session variable to detemine whether or not they must goto the next section or return to the index
  * page
  */
object SessionSectionHashUtil {

  /*
   * The current implementation of the hash is the hex decimal string of the sections bit field (flags),
   * where each section will occupy a no collision and designated bit.
   *
   * The bit is determined by the order of the section in the journey. e.g. Business details will always be the lowest
   * bit, where as suppliers will always be the highest bit. N.B. the value for supplier differs depending on the
   * business type, because the number of sections are different.
   *
   * example section hash:
   * let (a,b,c,d) be our section sequence, where a is the first section and d is the last
   * let 1 be section complete and 0 be section incomplete
   * then:
   *
   * (a=1,b=0,c=0,d=0) is binary 1    and hex 1
   * (a=0,b=1,c=0,d=1) is binary 1010 and hex A
   *
   */

  /*
   *  this method returns the hex string representation of the flags
   *  the session hash is updated in Index controller and utilised in SaveAndRoutable to determine the redirection of
   *  save and continue in the linear journey.
   */
  val toHash = (isCompleted: Seq[Boolean]) => isCompleted.zipWithIndex.foldLeft(0) {
    (accum, zipWithIndex) =>
      val (isCompleted, index) = zipWithIndex
      isCompleted match {
        case true => accum | (1 << index)
        case false => accum
      }
  }.toHexString

  /*
   *  this method decodes the section status hash and determine if a section (in its index form) was completed
   *  when this journey started
   */
  val isCompleted = (journeyIndex: Int, sectionStatusHashAtStartOfJourney: String) => {
    val statusHash: Int = Integer.parseInt(sectionStatusHashAtStartOfJourney, 16)
    statusHash & (1 << journeyIndex) match {
      case 0 => false
      case _ => true
    }
  }

}
