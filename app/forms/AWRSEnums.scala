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

package forms

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
    val DeRegistered: ApplicationStatusEnum.Value = Value(DeRegistrationString)
    val Withdrawn: ApplicationStatusEnum.Value = Value(WithdrawnString)
    val Rejected: ApplicationStatusEnum.Value = Value(RejectedString)
    val Revoked: ApplicationStatusEnum.Value = Value(RevokedString)
    val blankString: ApplicationStatusEnum.Value = Value("")
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
    val True: BooleanCheckboxEnum.Value = Value("true")
    val False: BooleanCheckboxEnum.Value = Value("false")

    // stable identifiers for usage in match functions
    val TrueString: String = True.toString
    val FalseString: String = False.toString

    def toBoolean(value: BooleanEnumeration#Value): Option[Boolean] = value match {
      case True => Some(true)
      case False => Some(false)
      case _ => None
    }
  }

  object BooleanRadioEnum extends BooleanEnumeration {
    val Yes: BooleanRadioEnum.Value = Value("Yes")
    val No: BooleanRadioEnum.Value = Value("No")

    // stable identifiers for usage in match functions
    val YesString: String = Yes.toString
    val NoString: String = No.toString

    def toBoolean(value: BooleanEnumeration#Value): Option[Boolean] = value match {
      case Yes => Some(true)
      case No => Some(false)
      case _ => None
    }

  }

  object DirectorAndSecretaryEnum extends AWRSEnumeration {
    val Director: DirectorAndSecretaryEnum.Value = Value("Director")
    val CompanySecretary: DirectorAndSecretaryEnum.Value = Value("Company Secretary")
    val DirectorAndSecretary: DirectorAndSecretaryEnum.Value = Value("Director and Company Secretary")

    def getText(`enum`:DirectorAndSecretaryEnum.Value) : String = (enum: @unchecked) match {
      case Director => "awrs.generic.status.director"
      case CompanySecretary => "awrs.generic.status.company_secretary"
      case DirectorAndSecretary => "awrs.generic.status.both"
    }

    def getMessageKey(enumStr: Option[String]) : String = enumStr.map(x => getText(withName(x))).getOrElse("")
  }

  object PersonOrCompanyEnum extends AWRSEnumeration {
    val Person: PersonOrCompanyEnum.Value = Value("person")
    val Company: PersonOrCompanyEnum.Value = Value("company")

    def getText(`enum`:PersonOrCompanyEnum.Value) :String = (enum: @unchecked) match {
      case Person => "awrs.generic.status.person"
      case Company => "awrs.generic.status.company"
    }

    def getMessageKey(enumStr: Option[String]) : String =  enumStr.map(x => getText(withName(x))).getOrElse("")
  }

  object EntityTypeEnum extends AWRSEnumeration {
    val Individual: EntityTypeEnum.Value = Value(IndividualString)
    val CorporateBody: EntityTypeEnum.Value = Value(CorporateBodyString)
    val SoleTrader: EntityTypeEnum.Value = Value(SoleTraderString)

    def getText(`enum`:EntityTypeEnum.Value) : String = (enum: @unchecked) match {
      case Individual => "awrs.business-partner.entityType_individual"
      case CorporateBody => "awrs.business-partner.entityType_corporate_body"
      case SoleTrader => "awrs.business-partner.entityType_sole_trader"
    }

    def getMessageKey(enumStr: Option[String]) : String =  enumStr.map(x => getText(withName(x))).getOrElse("")
  }

  object OperatingDurationEnum extends AWRSEnumeration {
    val ZeroToTwoYears: OperatingDurationEnum.Value = Value("Less than 2 years")
    val TwoToFiveYears: OperatingDurationEnum.Value = Value("2 to 4 years")
    val FiveToTenYears: OperatingDurationEnum.Value = Value("5 to 9 years")
    val TenPlusYears: OperatingDurationEnum.Value = Value("10 or more years")
  }

  object DeRegistrationReasonEnum extends AWRSEnumeration {
    val CeasesToBeRegisterableForTheScheme: DeRegistrationReasonEnum.Value = Value("Ceases to be registerable for the scheme")
    val CeasesToTradeAsAnAlcoholWholesaler: DeRegistrationReasonEnum.Value = Value("Ceases to trade as an alcohol wholesaler")
    val JoiningAGroupToRegisterForAWRS: DeRegistrationReasonEnum.Value = Value("Registering with a group")
    val JoiningAPartnershipToRegisterForAWRS: DeRegistrationReasonEnum.Value = Value("Registering with a partnership")
    val GroupDisbanded: DeRegistrationReasonEnum.Value = Value("Group ended")
    val PartnershipDisbanded: DeRegistrationReasonEnum.Value = Value("Partnership disbanded")
    val Other: DeRegistrationReasonEnum.Value = Value("Others")
  }

  object WithdrawalReasonEnum extends AWRSEnumeration {
    val AppliedInError: WithdrawalReasonEnum.Value = Value("Applied in error")
    val NoLongerTrading: WithdrawalReasonEnum.Value = Value("No Longer trading")
    val DuplicateApplication: WithdrawalReasonEnum.Value = Value("Duplicate Application")
    val JoinedAWRSGroup: WithdrawalReasonEnum.Value = Value("Joined AWRS Group")
    val Other: WithdrawalReasonEnum.Value = Value("Others")
  }

}
