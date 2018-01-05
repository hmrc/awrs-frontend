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

package view_models

import utils.AwrsUnitTestTraits

class IndexViewModelTest extends AwrsUnitTestTraits {

  val testModel = (status: IndexStatus) => SectionModel("", "", "", status)

  def testData(sessionsComplete: Boolean*) = IndexViewModel(sessionsComplete.map(
    complete => testModel(complete match {
      case true => SectionComplete
      case false => SectionIncomplete
    })).toList
  )

  "IndexViewModel" should {
    "calculate the correct hash for the session based on the sections completed" in {
      testData(true, false, false).toSessionHash shouldBe 1.toHexString
      testData(true, true, false).toSessionHash shouldBe 3.toHexString
      testData(false, false, true).toSessionHash shouldBe 4.toHexString
      testData(true, false, true).toSessionHash shouldBe 5.toHexString
      testData(true, true, true).toSessionHash shouldBe 7.toHexString
    }
  }
}
