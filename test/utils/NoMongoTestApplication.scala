/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.{APIShortLivedCacheRepository, SessionCacheRepository, ShortLivedCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.time.Instant

trait NoMongoTestApplication extends GuiceFakeApplicationFactory with MockitoSugar {

  override def fakeApplication(): Application = {
    val mockSessionCacheRepository = mock[SessionCacheRepository]
    val mockShortLivedCacheRepository = mock[ShortLivedCacheRepository]
    val mockAPIShortLivedCacheRepository = mock[APIShortLivedCacheRepository]
    val mockMongoComponent = mock[MongoComponent]
    val stubTimestampSupport = new TimestampSupport {
      override def timestamp(): Instant = Instant.now()
    }

    GuiceApplicationBuilder()
      .disable[config.MongoModule]
      .overrides(
        bind[MongoComponent].toInstance(mockMongoComponent),
        bind[TimestampSupport].toInstance(stubTimestampSupport),
        bind[SessionCacheRepository].toInstance(mockSessionCacheRepository),
        bind[ShortLivedCacheRepository].toInstance(mockShortLivedCacheRepository),
        bind[APIShortLivedCacheRepository].toInstance(mockAPIShortLivedCacheRepository)
      )
      .build()
  }
}
