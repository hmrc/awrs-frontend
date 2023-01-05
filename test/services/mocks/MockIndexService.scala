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

package services.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.IndexService
import utils.AwrsUnitTestTraits
import view_models.{IndexViewModel, SectionModel, SectionComplete}

import scala.concurrent.Future

trait MockIndexService extends AwrsUnitTestTraits {

  val mockIndexService: IndexService = mock[IndexService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIndexService)
  }

  import MockIndexService._

  protected final def setupMockIndexService(showOneViewLink: Boolean = defaultTrueBoolean,
                                            showContinueButton: Boolean = defaultTrueBoolean,
                                            getStatus: IndexViewModel = defaultindexViewModelComplete): Unit = {
    when(mockIndexService.showOneViewLink(ArgumentMatchers.any())).thenReturn(showOneViewLink)
    when(mockIndexService.showContinueButton(ArgumentMatchers.any())).thenReturn(showContinueButton)
    when(mockIndexService.getStatus(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(getStatus))
  }
}

object MockIndexService {
  val defaultTrueBoolean = true
  val defaultindexViewModelComplete = IndexViewModel(List(
    SectionModel("businessDetails", "/alcohol-wholesale-scheme/corporate-body-business-details", "awrs.index_page.business_details_text", SectionComplete),
    SectionModel("additionalPremises", "/alcohol-wholesale-scheme/additional-premises", "awrs.index_page.additional_premises_text", SectionComplete),
    SectionModel("additionalBusinessInformation", "/alcohol-wholesale-scheme/additional-information", "awrs.index_page.additional_business_information_text", SectionComplete),
    SectionModel("aboutYourSuppliers", "/alcohol-wholesale-scheme/supplier-addresses", "awrs.index_page.suppliers_text", SectionComplete),
    SectionModel("directorsAndCompanySecretaries", "/alcohol-wholesale-scheme/business-directors", "awrs.index_page.business_directors.index_text", SectionComplete))
  )
}
