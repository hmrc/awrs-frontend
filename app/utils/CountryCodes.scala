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

package utils

import models.{Address, Supplier}
import play.api.Environment
import play.api.libs.json.{JsValue, Json, OFormat}

import javax.inject.Inject
import scala.io.Source

class CountryCodes @Inject()(val environment: Environment) {

  case class Country(country: String, countryCode: String)

  object Country {
    implicit val formats: OFormat[Country] = Json.format[Country]
  }

  private[utils] val readJson: JsValue = {
    environment.resourceAsStream("country-code.json") match {
      case Some(inputStream) => Json.parse(Source.fromInputStream(inputStream, "UTF-8").mkString)
      case _                 => throw new Exception("Country codes file not found")
    }
  }

  val countryCodeList: Seq[Country] = readJson.validate[Seq[Country]].get
  val countryCodesTuplesSeq: Seq[(String, String)] = countryCodeList.map(
    x => (x.countryCode, x.country)
  ).sortBy(_._2)

  val countries: String = {
    Json.toJson(readJson.\\("country").toList.map(x => x.toString().replaceAll("\"", ""))).toString()
  }

  private val countryCodesMap: Map[String, String] = {
    countryCodeList.map(country => (country.countryCode, country.country)).toMap
  }

  private val countriesMap: Map[String, String] = {
    countryCodeList.map(country => (country.country.toLowerCase, country.countryCode)).toMap
  }

  def getCountry(countryCode: String): Option[String] = {
    countryCodesMap.get(countryCode)
  }

  def getCountryCode(country: String): Option[String] = {
    countriesMap.get(country.toLowerCase)
  }

  def getSupplierAddressWithCountryCode(supplierAddressesData: Supplier): Option[Address] = {
    val supplierAddress: Option[Address] = supplierAddressesData.supplierAddress match {
      case Some(address) =>
        val countryCode = getCountryCode(address.addressCountry.getOrElse("")) match {
          case Some(cc) => Some(cc)
          case _ => Some("GB")
        }
        Some(address.copy(addressCountryCode = countryCode))
      case _ => None
    }
    supplierAddress
  }

  def getSupplierAddressWithCountry(supplierAddressesData: Supplier): Option[Address] = {
    val supplierAddress: Option[Address] = supplierAddressesData.supplierAddress match {
      case Some(address) =>
        val country = getCountry(address.addressCountryCode.getOrElse("")) match {
          case Some(country) => Some(country)
          case _ => Some("United Kingdom")
        }
        Some(address.copy(addressCountry = country))
      case _ => None
    }
    supplierAddress
  }

  def getAddressWithCountryCode(addressesData: Option[Address]): Option[Address] =
    addressesData match {
      case Some(address) =>
        val countryCode = getCountryCode(address.addressCountry.getOrElse("")) match {
          case Some(cc) => Some(cc)
          case _ => Some("GB")
        }
        Some(address.copy(addressCountryCode = countryCode))
      case _ => None
    }

  def getAddressWithCountry(addressesData: Option[Address]): Option[Address] =
    addressesData match {
      case Some(address) =>
        val country = getCountry(address.addressCountryCode.getOrElse("")) match {
          case Some(country) => Some(country)
          case _ => Some("United Kingdom")
        }
        Some(address.copy(addressCountry = country))
      case _ => None
    }
}
