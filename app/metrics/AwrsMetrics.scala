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

package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.graphite.MicroserviceMetrics
import models.ApiType
import models.ApiType.ApiType

trait AwrsMetrics extends MicroserviceMetrics  {
  def startTimer(api: ApiType): Timer.Context

  def incrementSuccessCounter(api: ApiType.ApiType): Unit

  def incrementFailedCounter(api: ApiType.ApiType): Unit
}

object AwrsMetrics extends AwrsMetrics {

  val timers = Map(
    ApiType.API4Enrolment -> metrics.defaultRegistry.timer("api4-enrolment-response-timer"),
    ApiType.API10DeEnrolment -> metrics.defaultRegistry.timer("api10-de-enrolment-response-timer")
  )

  val successCounters = Map(
    ApiType.API4Enrolment -> metrics.defaultRegistry.counter("api4-enrolment-success"),
    ApiType.API10DeEnrolment -> metrics.defaultRegistry.counter("api10-de-enrolment-success")
  )

  val failedCounters = Map(
    ApiType.API4Enrolment -> metrics.defaultRegistry.counter("api4-enrolment-failed"),
    ApiType.API10DeEnrolment -> metrics.defaultRegistry.counter("api10-de-enrolment-failed")
  )

  override def startTimer(api: ApiType): Context = timers(api).time()

  override def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: ApiType): Unit = failedCounters(api).inc()
}
