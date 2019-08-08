/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.{Configuration, Play}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CryptoWithKeysFromConfig}
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

class CachedStaticHtmlPartialProvider @Inject()(val httpGet: DefaultHttpClient) extends CachedStaticHtmlPartialRetriever

class BusinessCustomerSessionCache @Inject()(servicesConfig: ServicesConfig,
                                             val http: DefaultHttpClient) extends SessionCache {
  override lazy val defaultSource: String = servicesConfig.getConfString("cachable.session-cache.review-details.cache", "business-customer-frontend")

  override lazy val baseUri: String = servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain: String = servicesConfig.getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

class AwrsSessionCache @Inject()(servicesConfig: ServicesConfig,
                                 val http: DefaultHttpClient) extends SessionCache {
  override lazy val defaultSource: String = servicesConfig.getConfString("cachable.session-cache.awrs-frontend.cache", "awrs-frontend")

  override lazy val baseUri: String = servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain: String = servicesConfig.getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

class AwrsShortLivedCaching @Inject()(servicesConfig: ServicesConfig,
                                      val http: DefaultHttpClient) extends ShortLivedHttpCaching {

  override lazy val defaultSource: String = servicesConfig.getConfString("cachableshort-lived-cache.awrs-frontend.cache", "awrs-frontend")
  override lazy val baseUri: String = servicesConfig.baseUrl("cachable.short-lived-cache")
  override lazy val domain: String = servicesConfig.getConfString("cachable.short-lived-cache.domain", throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

class AwrsAPIDataShortLivedCaching @Inject()(servicesConfig: ServicesConfig,
                                             val http: DefaultHttpClient) extends ShortLivedHttpCaching {

  override lazy val defaultSource: String = servicesConfig.getConfString("cachableshort-lived-cache.awrs-frontend-api.cache", "awrs-frontend-api")
  override lazy val baseUri: String = servicesConfig.baseUrl("cachable.short-lived-cache")
  override lazy val domain: String = servicesConfig.getConfString("cachable.short-lived-cache.domain", throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

class AwrsShortLivedCache @Inject()(awrsShortLivedCaching: AwrsShortLivedCaching) extends ShortLivedCache {
  override implicit lazy val crypto: CryptoWithKeysFromConfig = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
  override lazy val shortLiveCache: ShortLivedHttpCaching = awrsShortLivedCaching
}

class AwrsAPIShortLivedCache @Inject()(awrsAPIDataShortLivedCaching: AwrsAPIDataShortLivedCaching) extends ShortLivedCache {
  override implicit lazy val crypto: CryptoWithKeysFromConfig = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
  override lazy val shortLiveCache: ShortLivedHttpCaching = awrsAPIDataShortLivedCaching
}
