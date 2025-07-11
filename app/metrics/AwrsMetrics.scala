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

package metrics

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import models.ApiType
import models.ApiType.ApiType

class AwrsMetrics {

  val metricRegistry = new MetricRegistry

  val timers: Map[models.ApiType.Value, Timer] = Map(
    ApiType.API4Enrolment -> metricRegistry.timer("api4-enrolment-response-timer"),
    ApiType.API10DeEnrolment -> metricRegistry.timer("api10-de-enrolment-response-timer"),
    ApiType.ES9DeEnrolment -> metricRegistry.timer("es9-de-enrolment-response-timer"),
    ApiType.ES1Query -> metricRegistry.timer("es1-query-response-timer"),
    ApiType.ES20Query -> metricRegistry.timer("es20-query-response-timer")
  )

  val successCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.API4Enrolment -> metricRegistry.counter("api4-enrolment-success"),
    ApiType.API10DeEnrolment -> metricRegistry.counter("api10-de-enrolment-success"),
    ApiType.ES9DeEnrolment -> metricRegistry.counter("es9-de-enrolment-success"),
    ApiType.ES1Query -> metricRegistry.counter("es1-query-success"),
    ApiType.ES20Query -> metricRegistry.counter("es20-query-success")
  )

  val failedCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.API4Enrolment -> metricRegistry.counter("api4-enrolment-failed"),
    ApiType.API10DeEnrolment -> metricRegistry.counter("api10-de-enrolment-failed"),
    ApiType.ES9DeEnrolment -> metricRegistry.counter("es9-de-enrolment-failed"),
    ApiType.ES1Query -> metricRegistry.counter("es1-query-failed"),
    ApiType.ES20Query -> metricRegistry.counter("es20-query-failed")
  )

  def startTimer(api: ApiType): Context = timers(api).time()

  def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  def incrementFailedCounter(api: ApiType): Unit = failedCounters(api).inc()
}
