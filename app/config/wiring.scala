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

package config

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

import javax.inject.Inject
import scala.concurrent.duration.{Duration, DurationInt}

class CachedStaticHtmlPartialProvider @Inject()(val httpClientV2: HttpClientV2) extends CachedStaticHtmlPartialRetriever {
  override def refreshAfter: Duration = 60.seconds

  override def expireAfter: Duration = 60.minutes

  override def maximumEntries: Int = 1000
}

// Old http-caching-client classes removed - now using MongoDB repositories
// class BusinessCustomerSessionCache - replaced by SessionCacheRepository
// class AwrsSessionCache - replaced by SessionCacheRepository
// class AwrsShortLivedCache - replaced by ShortLivedCacheRepository
// class AwrsAPIShortLivedCache - replaced by APIShortLivedCacheRepository
