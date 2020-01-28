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

package config

import connectors.{BusinessMatchingConnector, BusinessMatchingConnectorImpl}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import utils.{CountryCodes, CountryCodesImpl}

class Bindings extends Module{
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    bindDeps() ++ bindConnectors()
  }

  private def bindDeps() = Seq(
    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient]),
    bind(classOf[CountryCodes]).to(classOf[CountryCodesImpl])
  )

  private def bindConnectors() = Seq(
    bind(classOf[BusinessMatchingConnector]).to(classOf[BusinessMatchingConnectorImpl])
  )
}
