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

import caching.ShortLivedCache
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.SessionCacheRepository
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


class BusinessCustomerSessionCache @Inject()(servicesConfig: ServicesConfig,
                                             mongoComponent: MongoComponent,
                                             timestampSupport: TimestampSupport)(implicit ec: ExecutionContext)
  extends SessionCacheRepository(
    mongoComponent = mongoComponent,
    collectionName = servicesConfig.getConfString("microservice.services.cachable.session-cache.review-details.cache", "business-customer-frontend"),
    ttl = Duration(servicesConfig.getInt("microservice.services.cachable.session-cache.timeToLiveInSeconds"), TimeUnit.SECONDS),
    timestampSupport = timestampSupport, sessionIdKey = SessionKeys.sessionId) {
}

class AwrsSessionCache @Inject()(servicesConfig: ServicesConfig,
                                 mongoComponent: MongoComponent,
                                 timestampSupport: TimestampSupport)(implicit ec: ExecutionContext) extends SessionCacheRepository(
  mongoComponent = mongoComponent,
  collectionName = servicesConfig.getConfString("microservice.services.cachable.session-cache.awrs-frontend.cache", "awrs-frontend"),
  ttl = Duration(servicesConfig.getInt("microservice.services.cachable.session-cache.timeToLiveInSeconds"), TimeUnit.SECONDS),
  timestampSupport = timestampSupport, sessionIdKey = SessionKeys.sessionId) {
}


class AwrsShortLivedCache @Inject()(servicesConfig: ServicesConfig,
                                    applicationCrypto: ApplicationCrypto,
                                    mongoComponent: MongoComponent,
                                    timestampSupport: TimestampSupport) (implicit ec: ExecutionContext) extends ShortLivedCache(
  mongoComponent = mongoComponent,
  collectionName = servicesConfig.getConfString("microservice.services.cachable.short-lived-cache-api.awrs-frontend.cache", "awrs-frontend-api"),
  ttl = Duration(servicesConfig.getInt("microservice.services.cachable.session-cache.timeToLiveInSeconds"), TimeUnit.SECONDS),
  timestampSupport = timestampSupport
) {
  override implicit lazy val crypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto
}

class AwrsAPIShortLivedCache @Inject()(servicesConfig: ServicesConfig,
                                       applicationCrypto: ApplicationCrypto,
                                       mongoComponent: MongoComponent,
                                       timestampSupport: TimestampSupport) (implicit ec: ExecutionContext) extends ShortLivedCache(
  mongoComponent = mongoComponent,
  collectionName = servicesConfig.getConfString("microservice.services.cachable.short-lived-cache-api.awrs-frontend.cache", "awrs-frontend-api"),
  ttl = Duration(servicesConfig.getInt("microservice.services.cachable.session-cache.timeToLiveInSeconds"), TimeUnit.SECONDS),
  timestampSupport = timestampSupport
) {
  override implicit lazy val crypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto
}
