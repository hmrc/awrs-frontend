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

package services

import connectors.EnrolmentStoreProxyConnector
import models.AwrsEnrolmentUtr
import models.reenrolment.AwrsRegisteredPostcode.sanitiseAndCompare
import models.reenrolment.{AwrsRegisteredPostcode, Identifier, KnownFact, KnownFacts, Verifier}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class EnrolmentStoreProxyService @Inject()(esConnector: EnrolmentStoreProxyConnector) {
  def queryForPrincipalGroupIdOfAWRSEnrolment(awrs: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    esConnector.queryForPrincipalGroupIdOfAWRSEnrolment(awrs)

  def verifyKnownFacts(arws: String, isSA: Boolean, utr: AwrsEnrolmentUtr, postCode: AwrsRegisteredPostcode)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val knownFacts = KnownFacts("HMRC-AWRS-ORG", Seq(KnownFact("AWRSRefNumber", arws)))
    esConnector.lookupEnrolments(knownFacts).map {
      case Some(esResponse) => esResponse.enrolments
        .find(_.identifiers.contains(Identifier("AWRSRefNumber", arws))).exists { enrolment =>
          println("enrolment: " + enrolment)
          enrolment.verifiers.contains(Verifier(if (isSA) "SAUTR" else "CTUTR", utr.utr)) &&
            enrolment.verifiers.exists(verifier => verifier.key == "Postcode" && sanitiseAndCompare(verifier.value, postCode.registeredPostcode))
        }
      case _ => false
    }
  }
}

