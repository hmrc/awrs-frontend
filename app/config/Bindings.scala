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

import connectors.{BusinessMatchingConnector, BusinessMatchingConnectorImpl}
import play.api.inject.{Binding, Module, bind => playBind}
import play.api.{Configuration, Environment}
import repositories.{AwrsApiShortLivedCacheRepositoryProvider, AwrsShortLivedCacheRepositoryProvider, CacheRepository, SessionCacheRepositoryProvider}
import uk.gov.hmrc.mongo.TimestampSupport

import java.time.Instant

class Bindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      playBind(classOf[BusinessMatchingConnector]).to(classOf[BusinessMatchingConnectorImpl])
    )
  }
}

class MongoModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      playBind(classOf[TimestampSupport]).toInstance(new TimestampSupport {
        override def timestamp(): Instant = Instant.now()
      }),
      playBind(classOf[CacheRepository])
        .qualifiedWith("session")
        .toProvider(classOf[SessionCacheRepositoryProvider]),
      playBind(classOf[CacheRepository])
        .qualifiedWith("awrs")
        .toProvider(classOf[AwrsShortLivedCacheRepositoryProvider]),
      playBind(classOf[CacheRepository])
        .qualifiedWith("awrs-api")
        .toProvider(classOf[AwrsApiShortLivedCacheRepositoryProvider])
    )
  }
}
