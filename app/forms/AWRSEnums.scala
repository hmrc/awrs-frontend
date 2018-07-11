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

package forms

import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current


object AWRSEnums {

  val DeRegistrationString = "De-registered"
  val WithdrawnString = "Withdrawn"

  val RejectedString = "Rejected"
  val RevokedString = "Revoked"

  val IndividualString = "Individual"
  val CorporateBodyString = "Corporate Body"
  val SoleTraderString = "Sole Trader"
  val LlpString = "LLP"

  trait AWRSEnumeration extends Enumeration {
    private lazy val stringValues = values.map(a => a.toString)

    final def isEnumValue(value: String): Boolean = stringValues.contains(value)
  }

  object ApplicationStatusEnum extends AWRSEnumeration {
    val DeRegistered = Value(DeRegistrationString)
    val Withdrawn = Value(WithdrawnString)
    val Rejected = Value(RejectedString)
    val Revoked = Value(RevokedString)
    val blankString = Value("")
  }

  trait BooleanEnumeration extends AWRSEnumeration {
    def toBoolean(value: BooleanEnumeration#Value): Option[Boolean]

    final def toBoolean(value: String): Option[Boolean] = {
      if (isEnumValue(value)) {
        toBoolean(this.withName(value))
      } else {
        None
      }
    }
  }

  object BooleanCheckboxEnum extends BooleanEnumeration {
    val True = Value("true")
    val False = Value("false")

    // stable identifiers for usage in match functions
    val TrueString = True.toString
    val FalseString = False.toString

    def toBoolean(value: BooleanEnumeration#Value): Option[Boolean] = value match {
      case True => Some(true)
      case False => Some(false)
      case _ => None
    }
  }

  object BooleanRadioEnum extends BooleanEnumeration {
    val Yes = Value("Yes")
    val No = Value("No")

    // stable identifiers for usage in match functions
    val YesString = Yes.toString
    val NoString = No.toString

    def toBoolean(value: BooleanEnumeration#Value): Option[Boolean] = value match {
      case Yes => Some(true)
      case No => Some(false)
      case _ => None
    }

  }

  object DirectorAndSecretaryEnum extends AWRSEnumeration {
    val Director = Value("Director")
    val CompanySecretary = Value("Company Secretary")
    val DirectorAndSecretary = Value("Director and Company Secretary")

    def getText(enum:DirectorAndSecretaryEnum.Value) :Option[String] = enum match {
      case Director => Some(Messages("awrs.generic.status.director"))
      case CompanySecretary => Some(Messages("awrs.generic.status.company_secretary"))
      case DirectorAndSecretary => Some(Messages("awrs.generic.status.both"))
      case _ => None
    }

    def getText(enumStr:String) :Option[String] = getText(withName(enumStr))
  }

  object PersonOrCompanyEnum extends AWRSEnumeration {
    val Person = Value("person")
    val Company = Value("company")

    def getText(enum:PersonOrCompanyEnum.Value) :Option[String] = enum match {
      case Person => Some(Messages("awrs.generic.status.person"))
      case Company => Some(Messages("awrs.generic.status.company"))
      case _ => None
    }

    def getText(enumStr:String) :Option[String] = getText(withName(enumStr))
  }

  object EntityTypeEnum extends AWRSEnumeration {
    val Individual = Value(IndividualString)
    val CorporateBody = Value(CorporateBodyString)
    val SoleTrader = Value(SoleTraderString)

    def getText(enum:EntityTypeEnum.Value) :Option[String] = enum match {
      case Individual => Some(Messages("awrs.business-partner.entityType_individual"))
      case CorporateBody => Some(Messages("awrs.business-partner.entityType_corporate_body"))
      case SoleTrader => Some(Messages("awrs.business-partner.entityType_sole_trader"))
      case _ => None
    }

    def getText(enumStr:String) :Option[String] = getText(withName(enumStr))
  }

  object OperatingDurationEnum extends AWRSEnumeration {
    val ZeroToTwoYears = Value(Messages("awrs.generic.0to2years.value"))
    val TwoToFiveYears = Value(Messages("awrs.generic.2to4years.value"))
    val FiveToTenYears = Value(Messages("awrs.generic.5to9years.value"))
    val TenPlusYears = Value(Messages("awrs.generic.10plusYears.value"))
  }

  object DeRegistrationReasonEnum extends AWRSEnumeration {
    val CeasesToBeRegisterableForTheScheme = Value(Messages("awrs.de_registration.reason.cases_to_be_registerable_for_the_scheme.schema_enum"))
    val CeasesToTradeAsAnAlcoholWholesaler = Value(Messages("awrs.de_registration.reason.ceases_to_trade_as_an_alcohol_wholesaler.schema_enum"))
    val JoiningAGroupToRegisterForAWRS = Value(Messages("awrs.de_registration.reason.joining_a_group_to_register_for_awrs.schema_enum"))
    val JoiningAPartnershipToRegisterForAWRS = Value(Messages("awrs.de_registration.reason.joining_a_partnership_to_register_for_awrs.schema_enum"))
    val GroupDisbanded = Value(Messages("awrs.de_registration.reason.group_disbanded.schema_enum"))
    val PartnershipDisbanded = Value(Messages("awrs.de_registration.reason.partnership_disbanded.schema_enum"))
    val Other = Value(Messages("awrs.de_registration.reason.other.schema_enum"))
  }

  object WithdrawalReasonEnum extends AWRSEnumeration {
    val AppliedInError = Value("Applied in error")
    val NoLongerTrading = Value("No Longer trading")
    val DuplicateApplication = Value("Duplicate Application")
    val JoinedAWRSGroup = Value("Joined AWRS Group")
    val Other = Value("Others")

/*    val AppliedInError = Value("Registered by mistake")
    val NoLongerTrading = Value("No longer trading as an alcohol wholesaler or producer")
    val DuplicateApplication = Value("Registered more than once")
    val JoinedAWRSGroup = Value("Registering with a group")
    val Other = Value("Others")*/
  }

}
