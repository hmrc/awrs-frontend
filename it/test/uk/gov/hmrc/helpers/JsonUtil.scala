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

package uk.gov.hmrc.helpers

object JsonUtil extends JsonUtil

trait JsonUtil {

  val llpJson: String = """{
                  |  "subscriptionTypeFrontEnd": {
                  |    "legalEntity": {
                  |      "legalEntity": "LLP"
                  |    },
                  |    "businessPartnerName" : "businessPartnerName",
                  |    "businessCustomerDetails": {
                  |      "businessName": "businessName",
                  |      "businessType": "Corporate Body",
                  |      "businessAddress": {
                  |        "line_1": "1 Example Street",
                  |        "line_2": "Example View",
                  |        "line_3": "Example Town",
                  |        "line_4": "Exampleshire",
                  |        "postcode": "ZZ1 1ZZ",
                  |        "country": "GB"
                  |      },
                  |      "sapNumber": "1234567890",
                  |      "safeId": "XE0001234567890",
                  |      "isAGroup": false,
                  |      "agentReferenceNumber": "JARN1234567"
                  |    },
                  |    "businessDetails": {
                  |      "tradingName": "Trading name",
                  |      "newAWBusiness": {
                  |        "newAWBusiness": "No"
                  |      },
                  |      "isBusinessIncorporated": "Yes",
                  |      "companyRegDetails": {
                  |        "companyRegistrationNumber": "1234",
                  |        "dateOfIncorporation": "01/01/2015"
                  |      },
                  |      "dateGrpRepJoined": "2004-12-12"
                  |    },
                  |    "businessRegistrationDetails" : {
                  |      "identification": {
                  |        "doYouHaveVRN": "Yes",
                  |        "vrn": "1234",
                  |        "doYouHaveUTR": "Yes",
                  |        "utr": "test"
                  |      }
                  |    },
                  |    "placeOfBusiness": {
                  |      "mainPlaceOfBusiness": "Yes",
                  |      "placeOfBusinessLast3Years": "Yes",
                  |      "operatingDuration": "30",
                  |      "modelVersion" : "1.0"
                  |    },
                  |    "businessContacts": {
                  |      "contactAddressSame": "Yes",
                  |      "contactFirstName": "first name",
                  |      "contactLastName": "last name",
                  |      "email": "Email@email.com",
                  |      "confirmEmail": "Email@email.com",
                  |      "telephone": "012345678901",
                  |      "modelVersion" : "1.0"
                  |    },
                  |    "partnership": {
                  |      "partners": [{
                  |        "entityType": "Individual",
                  |        "partnerAddress": {
                  |          "postcode": "ZZ1 1ZZ",
                  |          "addressLine1": "AddressLine1",
                  |          "addressLine2": "Addressline2",
                  |          "addressLine3": "Addressline3",
                  |          "addressLine4": "Addressline4"
                  |        },
                  |        "firstName": "firstName",
                  |        "lastName": "lastName",
                  |        "doYouHaveNino": "No",
                  |        "otherPartners": "Yes"
                  |      },
                  |        {
                  |          "entityType": "Corporate Body",
                  |          "partnerAddress": {
                  |            "postcode": "ZZ1 1ZZ",
                  |            "addressLine1": "AddressLine1",
                  |            "addressLine2": "Addressline2",
                  |            "addressLine3": "Addressline3",
                  |            "addressLine4": "Addressline4"
                  |          },
                  |          "companyName": "CompSecretaryCompFirst",
                  |          "tradingName": "Invalid Trading",
                  |          "isBusinessIncorporated": "No",
                  |          "doYouHaveVRN": "Yes",
                  |          "vrn": "1234",
                  |          "doYouHaveUTR": "No",
                  |          "otherPartners": "No"
                  |        }],
                  |      "modelVersion": "1.0"
                  |    },
                  |    "groupMemberDetails": {
                  |      "numberOfGrpMembers": "1",
                  |      "members": [
                  |        {
                  |          "names": {
                  |            "companyName": "Company Name",
                  |            "tradingName": "Trading Name"
                  |          },
                  |          "companyRegDetails": {
                  |            "isBusinessIncorporated": true,
                  |            "companyRegistrationNumber": "1234",
                  |            "dateOfIncorporation": "28/01/2012"
                  |          },
                  |          "groupJoiningDate": "12/12/2012",
                  |          "address": {
                  |            "addressLine1": "addressLine1",
                  |            "addressLine2": "addressLine2",
                  |            "postalCode": "ZZ1 1ZZ",
                  |            "countryCode": "GB"
                  |          },
                  |          "identification": {
                  |            "doYouHaveVRN": true,
                  |            "vrn": "1234",
                  |            "doYouHaveUTR": false
                  |          }
                  |        }
                  |      ],
                  |      "modelVersion": "1.0"
                  |    },
                  |    "additionalPremises": {
                  |      "premises": [
                  |        {
                  |          "additionalPremises": "Yes",
                  |          "additionalAddress": {
                  |            "postcode": "ZZ1 1ZZ",
                  |            "addressLine1": "addressLine1",
                  |            "addressLine2": "addressLine2"
                  |          },
                  |          "addAnother": "No"
                  |        }
                  |      ]
                  |    },
                  |    "tradingActivity": {
                  |      "wholesalerType": [
                  |        "06",
                  |        "01",
                  |        "02",
                  |        "03",
                  |        "04",
                  |        "05"
                  |      ],
                  |      "typeOfAlcoholOrders": [
                  |        "01",
                  |        "02",
                  |        "03",
                  |        "04"
                  |      ],
                  |      "doesBusinessImportAlcohol": "Yes",
                  |      "doYouExportAlcohol": "No"
                  |    },
                  |    "products": {
                  |      "mainCustomers": [
                  |        "01",
                  |        "02",
                  |        "03",
                  |        "04",
                  |        "05",
                  |        "06",
                  |        "07",
                  |        "08"
                  |      ],
                  |      "productType": [
                  |        "02",
                  |        "03",
                  |        "05",
                  |        "06",
                  |        "99"
                  |      ],
                  |      "otherProductType": "otherProductType"
                  |    },
                  |    "suppliers": {
                  |      "suppliers": [{
                  |        "alcoholSuppliers": "Yes",
                  |        "supplierName": "supplierName",
                  |        "vatRegistered": "Yes",
                  |        "vatNumber": "1234",
                  |        "supplierAddress": {
                  |          "postcode": "ZZ1 1ZZ",
                  |          "addressLine1": "addressLine1",
                  |          "addressLine2": "addressLine2"
                  |        },
                  |        "additionalSupplier": "Yes"
                  |      },
                  |        {
                  |          "alcoholSuppliers": "Yes",
                  |          "supplierName": "supplierName",
                  |          "vatRegistered": "No",
                  |          "supplierAddress": {
                  |            "postcode": "ZZ1 1ZZ",
                  |            "addressLine1": "addressLine1",
                  |            "addressLine2": "addressLine2"
                  |          },
                  |          "additionalSupplier": "No"
                  |        }]
                  |    },
                  |    "applicationDeclaration": {
                  |      "declarationName": "declarationName",
                  |      "declarationRole": "declarationRole"
                  |    },
                  |    "modelVersion": "1.0"
                  |  }
                  |}
                  |""".stripMargin
}


