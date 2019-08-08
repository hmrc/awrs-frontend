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

package metrics

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import models.ApiType
import models.ApiType.ApiType

class AwrsMetrics {

  val metricRegistry = new MetricRegistry

  val timers: Map[models.ApiType.Value, Timer] = Map(
    ApiType.API4Enrolment -> metricRegistry.timer("api4-enrolment-response-timer"),
    ApiType.API10DeEnrolment -> metricRegistry.timer("api10-de-enrolment-response-timer")
  )

  val successCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.API4Enrolment -> metricRegistry.counter("api4-enrolment-success"),
    ApiType.API10DeEnrolment -> metricRegistry.counter("api10-de-enrolment-success")
  )

  val failedCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.API4Enrolment -> metricRegistry.counter("api4-enrolment-failed"),
    ApiType.API10DeEnrolment -> metricRegistry.counter("api10-de-enrolment-failed")
  )

  def startTimer(api: ApiType): Context = timers(api).time()

  def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  def incrementFailedCounter(api: ApiType): Unit = failedCounters(api).inc()
}
