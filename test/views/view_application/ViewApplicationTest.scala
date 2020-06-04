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

package views.view_application

import audit.Auditable
import config.ApplicationConfig
import connectors.mock.MockAuthConnector
import controllers.BusinessDirectorsController
import forms.AWRSEnums.{BooleanRadioEnum, DirectorAndSecretaryEnum, EntityTypeEnum, PersonOrCompanyEnum}
import forms.AwrsFormFields
import javax.inject.Inject
import models.BusinessDetailsEntityTypes._
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.DataCacheKeys._
import services.Save4LaterService
import org.mockito.Mockito._
import services.mocks.MockSave4LaterService
import uk.gov.hmrc.http.cache.client._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.TestUtil._
import utils.TestConstants._
import utils.{AccountUtils, AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}
import views.view_application.helpers.{OneViewMode, PrintFriendlyMode}
import views.view_application.subviews.SubviewIds._

import scala.collection.JavaConversions._
import scala.concurrent.Future


class ViewApplicationTest extends AwrsUnitTestTraits with MockAuthConnector with MockSave4LaterService {

  val mockDataCacheService = mock[Save4LaterService]

  override def beforeEach(): Unit = {
    reset(mockCountryCodes)

    super.beforeEach()
  }

  class TestController @Inject()(override val mcc: MessagesControllerComponents,
                                 override val save4LaterService: Save4LaterService,
                                 override val authConnector: DefaultAuthConnector,
                                 override val auditable: Auditable,
                                 override val accountUtils: AccountUtils,
                                 override implicit val applicationConfig: ApplicationConfig) extends BusinessDirectorsController(mcc, save4LaterService, mockDeEnrolService, authConnector, auditable, accountUtils, applicationConfig) {
    val status = "does not matter"

    def viewApplicationContent(dataCache: CacheMap, status: String)(implicit request: Request[AnyContent]): Boolean => HtmlFormat.Appendable =
      (printFriendly: Boolean) => {
        when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
          .thenReturn(true)

        if (printFriendly) {
          views.html.view_application.awrs_view_application_core(dataCache, status, TestUtil.defaultEnrolmentSet, mockAccountUtils)(viewApplicationType = PrintFriendlyMode, implicitly, messages, implicitly)
        } else {
          views.html.view_application.awrs_view_application_core(dataCache, status, TestUtil.defaultEnrolmentSet, mockAccountUtils)(viewApplicationType = OneViewMode, implicitly, messages, implicitly)
        }
      }

    def show(dataCache: CacheMap): Action[AnyContent] = Action.async {
      implicit request =>
        Future.successful(Ok(views.html.view_application.awrs_view_application(viewApplicationContent(dataCache, status), printFriendly = true, None, None)(implicitly, messages= messages, applicationConfig = mockAppConfig)))
    }
  }

  val testController: TestController =
    new TestController(mockMCC, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig){
      override val signInUrl = "/sign-in"
    }

  val request = FakeRequest()


  def getCustomizedMap(businessType: Option[BusinessType] = testBusinessDetailsEntityTypes(CorporateBody),
                       businessCustomerDetails: Option[BusinessCustomerDetails] = None,
                       businessDetails: Option[BusinessDetails] = None,
                       businessRegistrationDetails: Option[BusinessRegistrationDetails] = None,
                       placeOfBusiness: Option[PlaceOfBusiness] = None,
                       businessContacts: Option[BusinessContacts] = None,
                       partnerDetails: Option[Partners] = None,
                       groupMembers: Option[GroupMembers] = None,
                       additionalBusinessPremises: Option[AdditionalBusinessPremisesList] = None,
                       businessDirectors: Option[BusinessDirectors] = None,
                       tradingActivity: Option[TradingActivity] = None,
                       products: Option[Products] = None,
                       suppliers: Option[Suppliers] = None,
                       applicationDeclaration: Option[ApplicationDeclaration] = None,
                       businessNameDetails: Option[BusinessNameDetails] = None,
                       tradingStartDetails: Option[NewAWBusiness] = None
                      ) = {
    val id = testUtr
    val cacheMap = Map[String, JsValue]() ++
      prepMap[BusinessType](businessTypeName, businessType) ++
      prepMap[BusinessDetails](businessDetailsName, businessDetails) ++
      prepMap[BusinessRegistrationDetails](businessRegistrationDetailsName, businessRegistrationDetails) ++
      prepMap[PlaceOfBusiness](placeOfBusinessName, placeOfBusiness) ++
      prepMap[BusinessContacts](businessContactsName, businessContacts) ++
      prepMap[BusinessCustomerDetails](businessCustomerDetailsName, businessCustomerDetails) ++
      prepMap[Partners](partnersName, partnerDetails) ++
      prepMap[AdditionalBusinessPremisesList](additionalBusinessPremisesName, additionalBusinessPremises) ++
      prepMap[BusinessDirectors](businessDirectorsName, businessDirectors) ++
      prepMap[TradingActivity](tradingActivityName, tradingActivity) ++
      prepMap[Products](productsName, products) ++
      prepMap[Suppliers](suppliersName, suppliers) ++
      prepMap[ApplicationDeclaration](applicationDeclarationName, applicationDeclaration) ++
      prepMap[GroupMembers](groupMembersName, groupMembers) ++
      prepMap[BusinessNameDetails](businessNameDetailsName, businessNameDetails) ++
      prepMap[NewAWBusiness](tradingStartDetailsName, tradingStartDetails)

    CacheMap(id, cacheMap)
  }

  def prepMap[T](key: String, optionParam: Option[T])(implicit format: Format[T]) =
    optionParam match {
      case Some(param) => Map[String, JsValue](key -> Json.toJson(param))
      case _ => Map[String, JsValue]()
    }

  implicit def conv(item: String): List[String] = List(item)

  implicit def conv(someItem: Option[String]): List[String] = someItem match {
    case Some(item) => List(item)
    case _ => List()
  }

  implicit def conv(newRow: Row): List[Row] = List(newRow)

  implicit def conv(tupleDate: TupleDate): List[String] = List(tupleDate.toString("dd MMMM yyyy"))


  def utrMessage(businessType: String) = businessType match {
    case "SOP" => Messages("awrs.generic.do_you_have_sa_UTR")
    case "Partnership" | "LP" | "LLP" | "LLP_GRP" => Messages("awrs.generic.do_you_have_partnership_UTR")
    case _ => Messages("awrs.generic.do_you_have_CT_UTR")
  }


  def businessRegistrationDetailsToExpectation(someBusinessType: Option[String], testBusinessRegistrationDetails: Option[BusinessRegistrationDetails]): List[Row] = {
    (someBusinessType, testBusinessRegistrationDetails) match {
      case (Some(businessType), Some(id)) =>
        prepRow(Messages("awrs.generic.UTR_number"), id.utr) ++
          prepRow(Messages("awrs.generic.NINO"), id.nino) ++
          prepRowCustom(Messages("awrs.generic.company_reg"), id.companyRegDetails)(id.companyRegDetails.get.companyRegistrationNumber) ++
          prepRowCustom(Messages("awrs.generic.date_of_incorporation"), id.companyRegDetails)(id.companyRegDetails.get.dateOfIncorporation) ++
          prepRow(Messages("awrs.generic.do_you_have_VAT"), id.doYouHaveVRN) ++
          prepRow(Messages("awrs.generic.VAT_registration_number"), id.vrn)
      case _ => List()
    }
  }

  def addressToExpectation(heading: String, testAddress: Option[Address]): List[Row] = {
    def prepList(someStr: Option[String]): List[String] = someStr match {
      case Some(str) => List(str)
      case _ => List()
    }

    testAddress match {
      case Some(address) =>
        Row(heading,
          List(address.addressLine1,
            address.addressLine2) ++
            prepList(address.addressLine3) ++
            prepList(address.addressLine4) ++
            address.postcode ++
            address.addressCountry
        )
      case _ => List()
    }
  }

  def addressToExpectation(heading: String, testAddress: BCAddress): List[Row] = {
    def prepList(someStr: Option[String]): List[String] = someStr match {
      case Some(str) => List(str)
      case _ => List()
    }

    Row(heading,
      List(testAddress.line_1,
        testAddress.line_2) ++
        prepList(testAddress.line_3) ++
        prepList(testAddress.line_4) ++
        testAddress.postcode ++
        testAddress.country
    )
  }

  def addressToExpectationBC(heading: String, testAddress: Option[BCAddress]): List[Row] = {
    def prepList(someStr: Option[String]): List[String] = someStr match {
      case Some(str) => List(str)
      case _ => List()
    }

    testAddress match {
      case Some(address) =>
        Row(heading,
          List(address.line_1,
            address.line_2) ++
            prepList(address.line_3) ++
            prepList(address.line_4) ++
            address.postcode
        )
      case _ => List()
    }
  }

  def multipleChoicesToExpectation(heading: String, rawChoiceList: List[String], otherText: Option[String] = None)(transform: Map[String, String]): List[Row] = {
    val choices: List[String] = rawChoiceList.map(x => transform(x)).foldLeft(List[String]()) {
      (list, x) =>
        x match {
          case "All" => list
          case "Other" => list :+ ("Other: " + otherText.get)
          case x@_ => list :+ x
        }
    }
    Row(heading, choices)
  }

  def optMultipleChoicesToExpectation(heading: String, rawChoiceList: Option[List[String]], otherText: Option[String] = None)(transform: Map[String, String]): List[Row] = rawChoiceList match {
    case Some(choiceList) =>
      multipleChoicesToExpectation(heading, choiceList, otherText)(transform)
    case _ => List[Row]()
  }

  def prepRow(heading: String, ensureExists: Option[String]): List[Row] =
    ensureExists match {
      case Some(data) => List(Row(heading, data))
      case _ => List[Row]()
    }

  def prepRow(heading: String, ensureExists: List[Option[String]]): List[Row] =
    List(Row(heading, ensureExists.foldLeft(List[String]()) {
      (list: List[String], someStr: Option[String]) =>
        someStr match {
          case Some(str) => list :+ str
          case _ => list
        }
    }))

  def prepRowCustom[T](heading: String, ensureExists: Option[T]*)(r: => List[String]): List[Row] =
    ensureExists.forall(x => x.isDefined) match {
      case true => List(Row(heading, r))
      case _ => List[Row]()
    }

  def tableHeaderForContainers[T](header: String, container: Traversable[T]) = f"$header (${container.size})"

  "view-application page" should {

    "display business details correctly" in {
      def testDataOne(legalEntity: Option[String]) = testBusinessNameDetails()

      def testDataTwo(legalEntity: Option[String]) = testBusinessNameDetails()

      val testAWBusiness = NewAWBusiness(
        "Yes",
        Some(TupleDate("20", "1", "2015"))
      )

      val testDataSeqPartnership = Seq(testDataOne(Some("Partnership")), testDataTwo(Some("Partnership")))
      val testDataSeqGroup = Seq(testDataOne(Some("LLP_GRP")), testDataTwo(Some("LLP_GRP")))
      val testDataSeqBusDetails = Seq(testDataOne(Some("SoleTrader")), testDataTwo(Some("SoleTrader")))

      val bcd = testBusinessCustomerDetails("SOP")

      val entitiesPartnership = Seq(testBusinessDetailsEntityTypes(Partnership),
        testBusinessDetailsEntityTypes(Lp),
        testBusinessDetailsEntityTypes(Llp))
      val entitiesGroup = Seq(
        testBusinessDetailsEntityTypes(GroupRep))
      val entitiesBusDetails = Seq(testBusinessDetailsEntityTypes(CorporateBody),
        testBusinessDetailsEntityTypes(SoleTrader))
      entitiesPartnership.foreach(entity => testDataSeqPartnership.foreach(testData => test(entity, testData)))
      entitiesGroup.foreach(entity => testDataSeqGroup.foreach(testData => test(entity, testData)))
      entitiesBusDetails.foreach(entity => testDataSeqBusDetails.foreach(testData => test(entity, testData)))

      def toExpectation(testData: BusinessNameDetails)(entityType: String): List[Row] = {
        def expectedBusinessType =
          entityType match {
            case "Partnership" => Messages("awrs.business_details.business_partnership")
            case "LP" => Messages("awrs.business_details.limited_partnership_body")
            case "LLP" | "LLP_GRP" => Messages("awrs.business_details.limited_liability_partnership_body")
            case "LTD" | "LTD_GRP" => Messages("awrs.business_details.corporate_body")
            case "SOP" => Messages("awrs.business_details.sole_trader")
            case _ => Messages("awrs.business_details.unknown_body")
          }

        Row("", None) ++
          Row(Messages("awrs.business_details.name"), bcd.businessName) ++
          Row(Messages("awrs.business_details.type_of_business"), expectedBusinessType) ++
          prepRow(Messages("awrs.generic.trading_name"), testData.tradingName) ++
          Row(Messages("awrs.business_details.new_AWBusiness"), testAWBusiness.newAWBusiness) ++
          Row(Messages("awrs.business_details.start_date"), testAWBusiness.proposedStartDate.get.toString("dd MMMM yyyy"))
      }

      def test(entity: BusinessType, testData: BusinessNameDetails) = {
        implicit val divId = businessDetailsId

        implicit val cache =
          getCustomizedMap(businessNameDetails = testData,
            businessCustomerDetails = bcd,
            tradingStartDetails = testAWBusiness,
            businessType = entity)
        implicit val doc = getDoc(entity.legalEntity.get)
        val subview = getSubview

        testSectionExists(businessDetails = true)

        val expectedHeading = entity.legalEntity.get match {
          case "Partnership" | "LP" | "LLP" => Messages(f"awrs.view_application.partnership_details_text")
          case "LLP_GRP" | "LTD_GRP" => Messages(f"awrs.view_application.group_business_details_text")
          case _ => Messages(f"awrs.view_application.business_details_text")
        }

        val expected = toExpectation(testData)(entity.legalEntity.get)
        val sizeMatch = subview.rows.size == expected.size

        val debugInfo = s"sizematch=$sizeMatch\n" + {
          sizeMatch match {
            case true =>
              val listEleMatch: List[String] = (subview.rows zip expected).map {
                case (a: Row, b: Row) =>
                  val equal = a.equals(b)
                  equal.toString + "\t" + {
                    equal match {
                      case true => ""
                      case false => a.heading.toCharArray.map(_.toInt.toString) + "\t" + b.heading.toCharArray.map(_.toInt.toString) + "\n\t" +
                        a.content.map(_.toCharArray.map(_.toInt.toString)) + "\t" + b.content.map(_.toCharArray.map(_.toInt.toString)) + "\n"
                    }
                  }
              }
              listEleMatch.mkString(",\n") + "\n\n"
            case _ => ""
          }
        }
        withClue(debugInfo) {
          subview.heading shouldBe expectedHeading
          subview.rows shouldBe toExpectation(testData)(entity.legalEntity.get).drop(1)
        }
      }

    }

    "display business registration details correctly" in {
      def testDataOne(legalEntity: Option[String]) = (legalEntity, testBusinessRegistrationDetails(
        doYouHaveNino = Some("Yes"),
        nino = testNino,
        doYouHaveVRN = Some("Yes"),
        vrn = testVrn,
        doYouHaveUTR = Some("No")))

      def testDataTwo(legalEntity: Option[String]) = (legalEntity, testBusinessRegistrationDetails(
        isBusinessIncorporated = "Yes",
        companyRegDetails = Some(CompanyRegDetails(companyRegistrationNumber =
          "12343454", dateOfIncorporation = TupleDate("09", "06", "1985"))),
        doYouHaveVRN = Some("Yes"),
        vrn = testVrn,
        doYouHaveUTR = Some("No")))

      val testDataSeqPartnership = Seq(testDataOne(Some("Partnership")), testDataTwo(Some("Partnership")))
      val testDataSeqGroup = Seq(testDataOne(Some("LLP_GRP")), testDataTwo(Some("LLP_GRP")))
      val testDataSeqBusDetails = Seq(testDataOne(Some("SoleTrader")), testDataTwo(Some("SoleTrader")))

      val bcd = testBusinessCustomerDetails("SOP")

      val entitiesPartnership = Seq(testBusinessDetailsEntityTypes(Partnership),
        testBusinessDetailsEntityTypes(Lp),
        testBusinessDetailsEntityTypes(Llp))
      val entitiesGroup = Seq(
        testBusinessDetailsEntityTypes(GroupRep))
      val entitiesBusDetails = Seq(testBusinessDetailsEntityTypes(CorporateBody),
        testBusinessDetailsEntityTypes(SoleTrader))
      entitiesPartnership.foreach(entity => testDataSeqPartnership.foreach(testData => test(entity, testData._2)))
      entitiesGroup.foreach(entity => testDataSeqGroup.foreach(testData => test(entity, testData._2)))
      entitiesBusDetails.foreach(entity => testDataSeqBusDetails.foreach(testData => test(entity, testData._2)))

      def toExpectation(entity: Option[String], testData: BusinessRegistrationDetails): List[Row] =
        Row("", None) ++
          businessRegistrationDetailsToExpectation(entity, testData)

      def test(entity: BusinessType, testData: BusinessRegistrationDetails) = {
        implicit val divId = businessRegistrationDetailsId

        implicit val cache =
          getCustomizedMap(businessRegistrationDetails = testData,
            businessCustomerDetails = bcd,
            businessType = entity)
        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(businessRegistrationDetails = true)

        val expectedHeading = entity.legalEntity.get match {
          case "Partnership" | "LP" | "LLP" => Messages(f"awrs.view_application.partnership_registration_details_text")
          case "LLP_GRP" | "LTD_GRP" => Messages(f"awrs.view_application.group_business_registration_details_text")
          case _ => Messages(f"awrs.view_application.business_registration_details_text")
        }

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(entity.legalEntity, testData).drop(1)
      }

    }

    "display place of business correctly" in {
      implicit val divId = placeOfBusinessId
      val testData = testPlaceOfBusinessDefault()

      def toExpectation(testData: PlaceOfBusiness, testBCDetails: BusinessCustomerDetails): List[Row] =
        Row("", None) ++
          addressToExpectation(Messages("awrs.view_application.principal_place_business"), testData.mainAddress) ++
          prepRow(Messages("awrs.business_contacts.place_of_business_changed_last_3_years"), testData.placeOfBusinessLast3Years) ++
          addressToExpectation(Messages("awrs.view_application.previous_address"), testData.placeOfBusinessAddressLast3Years) ++
          prepRow(Messages("awrs.business_contacts.business_operating_from_current_address"), testData.operatingDuration)


      def test(entity: BusinessType, testData: PlaceOfBusiness) = {
        implicit val cache =
          getCustomizedMap(placeOfBusiness = testData,
            businessType = entity,
            businessCustomerDetails = testBusinessCustomerDetails(entity.legalEntity.get))

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(placeOfBusiness = true)

        val expectedHeading = entity.legalEntity.get match {
          case "Partnership" | "LP" | "LLP" => Messages(f"awrs.view_application.partnership_place_of_business_text")
          case "LLP_GRP" | "LTD_GRP" => Messages(f"awrs.view_application.group_place_of_business_text")
          case _ => Messages(f"awrs.view_application.place_of_business_text")
        }

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData, testBusinessCustomerDetails(entity.legalEntity.get)).drop(1)
      }

      val entities = Seq(testBusinessDetailsEntityTypes(Partnership),
        testBusinessDetailsEntityTypes(Lp),
        testBusinessDetailsEntityTypes(Llp),
        testBusinessDetailsEntityTypes(GroupRep),
        testBusinessDetailsEntityTypes(CorporateBody))
      entities.foreach(entity => test(entity, testData))
    }

    "display business contacts correctly" in {
      implicit val divId = businessContactsId
      val testData = List[BusinessContacts](testBusinessContactsDefault(), testBusinessContactsDefault(contactAddressSame = Some(BooleanRadioEnum.NoString)))

      def toExpectation(testData: BusinessContacts, testBCDetails: BusinessCustomerDetails): List[Row] = {
        val contactAddress =
          testData.contactAddressSame match {
            case Some(BooleanRadioEnum.NoString) => addressToExpectation(Messages("awrs.view_application.contact_address"), testData.contactAddress)
            case Some(BooleanRadioEnum.YesString) => addressToExpectation(Messages("awrs.view_application.contact_address"), testBCDetails.businessAddress)
          }

        Row("", None) ++
          prepRowCustom(Messages("awrs.view_application.contact_name"), testData.contactFirstName, testData.contactLastName)(testData.contactFirstName.get + " " + testData.contactLastName.get) ++
          prepRow(Messages("awrs.generic.telephone"), testData.telephone) ++
          prepRow(Messages("awrs.generic.email"), testData.email) ++
          contactAddress
      }

      def test(entity: BusinessType, testData: BusinessContacts) = {
        implicit val cache =
          getCustomizedMap(businessContacts = testData,
            businessType = entity,
            businessCustomerDetails = testBusinessCustomerDetails(entity.legalEntity.get))

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(businessContacts = true)

        val expectedHeading = entity.legalEntity.get match {
          case "Partnership" | "LP" | "LLP" => Messages(f"awrs.view_application.partnership_contacts_text")
          case "LLP_GRP" | "LTD_GRP" => Messages(f"awrs.view_application.group_business_contacts_text")
          case _ => Messages(f"awrs.view_application.business_contacts_text")
        }

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData, testBusinessCustomerDetails(entity.legalEntity.get)).drop(1)
      }

      val entities = Seq(testBusinessDetailsEntityTypes(Partnership),
        testBusinessDetailsEntityTypes(Lp),
        testBusinessDetailsEntityTypes(Llp),
        testBusinessDetailsEntityTypes(GroupRep),
        testBusinessDetailsEntityTypes(CorporateBody))
      entities.foreach(entity => testData.foreach(testEntry => test(entity, testEntry)))

    }

    "display group members details correctly" in {
      implicit val divId = groupMemberDetailsId

      val testData = GroupMembers(
        List(GroupMember(companyNames = CompanyNames(Some("ACME"), Some("Yes"), Some("Business1")), address = Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), groupJoiningDate = None, doYouHaveUTR = Some("Yes"), utr = testUtr, isBusinessIncorporated = Some("No"), companyRegDetails = Some(
          CompanyRegDetails(
            companyRegistrationNumber = testCrn,
            dateOfIncorporation = TupleDate("09", "06", "1985")
          )
        ), doYouHaveVRN = Some("Yes"), vrn = testVrn, addAnotherGrpMember = Some("No")),
          GroupMember(companyNames = CompanyNames(Some("ACME"), Some("Yes"), Some("Business1")), address = Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), groupJoiningDate = None, doYouHaveUTR = Some("Yes"), utr = testUtr, isBusinessIncorporated = Some("No"), companyRegDetails = Some(
            CompanyRegDetails(
              companyRegistrationNumber = testCrn,
              dateOfIncorporation = TupleDate("09", "06", "1985")
            )
          ), doYouHaveVRN = Some("Yes"), vrn = testVrn, addAnotherGrpMember = Some("No"))))

      def toExpectation(testData: GroupMembers): List[Row] = {
        def toList(testData: GroupMember): List[Row] = {
          def companyOrTradingName: String = testData.companyNames.businessName.fold(testData.companyNames.tradingName)(x => x).fold("")(x => x)

          def identificationToExpectation(groupMember: GroupMember): List[Row] =
            prepRow(Messages("awrs.generic.do_they_have_CT_UTR"), groupMember.doYouHaveUTR) ++
              prepRow(Messages("awrs.generic.UTR_number"), groupMember.utr) ++
              prepRow(Messages("awrs.generic.do_they_have_company_reg"), groupMember.isBusinessIncorporated) ++
              prepRowCustom(Messages("awrs.generic.company_reg"), groupMember.companyRegDetails)(groupMember.companyRegDetails.get.companyRegistrationNumber) ++
              prepRowCustom(Messages("awrs.generic.date_of_incorporation"), groupMember.companyRegDetails)(groupMember.companyRegDetails.get.dateOfIncorporation) ++
              prepRow(Messages("awrs.generic.do_they_have_VAT"), groupMember.doYouHaveVRN) ++
              prepRow(Messages("awrs.generic.VAT_registration_number"), groupMember.vrn)

          // company name takes precedence, and at least one must be present
          prepRow(companyOrTradingName, List[Option[String]](None, None)) ++
            // trading name will only be displayed if it's not already displayed above
            prepRowCustom(Messages("awrs.generic.trading_name"), testData.companyNames.businessName)(testData.companyNames.tradingName) ++
            addressToExpectation(Messages("awrs.generic.address"), testData.address) ++
            identificationToExpectation(testData) ++
            Row("", None)
        }

        // .dropRight(1) is added because the last row of the table does not have a record spacer
        testData.members.tail.flatMap(x => toList(x)).dropRight(1)
      }

      def test(testData: GroupMembers) {
        implicit val cache =
          getCustomizedMap(groupMembers = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(groupMembers = true)

        val expectedHeading = tableHeaderForContainers(Messages("awrs.view_application.group_member_details_text"), testData.members.tail)

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData)
      }

      test(testData)
    }

    "display partner details correctly" in {
      implicit val divId = partnerDetailsId

      def partnerDetailOneViewTest(entityType: Option[String] = Some("Individual"),
                                   firstName: Option[String] = "John",
                                   lastName: Option[String] = "Smith",
                                   companyName: Option[String] = "comapany name",
                                   tradingName: Option[String] = "trading name",
                                   partnerAddress: Option[Address] = testAddress(),
                                   doYouHaveNino: Option[String] = "Yes",
                                   nino: Option[String] = testNino,
                                   isBusinessIncorporated: Option[String] = "Yes",
                                   companyRegDetails: Option[CompanyRegDetails] = CompanyRegDetails(
                                     companyRegistrationNumber = testCrn,
                                     dateOfIncorporation = TupleDate("09", "06", "1985")),
                                   doYouHaveVRN: Option[String] = "Yes",
                                   vrn: Option[String] = testVrn,
                                   doYouHaveUTR: Option[String] = "Yes",
                                   utr: Option[String] = testUtr,
                                   otherPartners: Option[String] = Some("Yes")): Partner = {

        testPartner(entityType,
          firstName, lastName,
          companyName, tradingName,
          partnerAddress,
          doYouHaveNino, nino,
          isBusinessIncorporated, companyRegDetails,
          doYouHaveVRN, vrn,
          doYouHaveUTR, utr,
          otherPartners)
      }

      val testData = Partners(
        List(
          partnerDetailOneViewTest(
            entityType = "Individual", companyName = None, tradingName = None,
            isBusinessIncorporated = "No", companyRegDetails = None),
          partnerDetailOneViewTest(
            entityType = Some("Corporate Body"), firstName = None, lastName = None,
            doYouHaveNino = None, nino = None),
          partnerDetailOneViewTest(
            entityType = Some("Sole Trader"), companyName = None,
            isBusinessIncorporated = "No", companyRegDetails = None, otherPartners = Some("No"))
        )
      )

      def toExpectation(testData: Partners): List[Row] = {
        def toList(testData: Partner, index: Int): List[Row] = {
          def sectionHeader = index match {
            case 0 => prepRow(Messages("awrs.business-partner.partner"), List(None))
            case 1 => prepRow(Messages("awrs.business-partner.additional_partners"), List(None))
            case _ => prepRow("", None)
          }

          def nameOrCompanyOrTradingName: List[Row] =
            testData.entityType.get.toLowerCase match {
              case "individual" =>
                val name = testData.firstName.get + " " + testData.lastName.get
                Row(name, None)
              case "corporate body" =>
                val name = testData.companyNames.fold("")(x => x.businessName.fold("")(x => x))
                Row(name, None) ++
                  prepRow(Messages("awrs.generic.trading_name"), testData.companyNames.tradingName)
              case "sole trader" =>
                val name = testData.firstName.get + " " + testData.lastName.get
                Row(name, None) ++
                  prepRow(Messages("awrs.generic.trading_name"), testData.companyNames.tradingName)
            }

          def identificationToExpectation(partner: Partner): List[Row] =
            prepRow(Messages("awrs.generic.do_they_have_partnership_UTR"), partner.doYouHaveUTR) ++
              prepRow(Messages("awrs.generic.UTR_number"), partner.utr) ++
              prepRow(Messages("awrs.generic.do_they_have_NINO"), partner.doYouHaveNino) ++
              prepRow(Messages("awrs.generic.NINO"), partner.nino) ++
              prepRow(Messages("awrs.generic.do_they_have_company_reg"), partner.isBusinessIncorporated) ++
              prepRowCustom(Messages("awrs.generic.company_reg"), partner.companyRegDetails)(partner.companyRegDetails.get.companyRegistrationNumber) ++
              prepRowCustom(Messages("awrs.generic.date_of_incorporation"), partner.companyRegDetails)(partner.companyRegDetails.get.dateOfIncorporation) ++
              prepRow(Messages("awrs.generic.do_they_have_VAT"), partner.doYouHaveVRN) ++
              prepRow(Messages("awrs.generic.VAT_registration_number"), partner.vrn)

          sectionHeader ++
            nameOrCompanyOrTradingName ++
            prepRowCustom(Messages("awrs.business-partner.partner_role"), testData.entityType)(EntityTypeEnum.getMessageKey(testData.entityType.get)) ++
            addressToExpectation(Messages("awrs.generic.address"), testData.partnerAddress) ++
            identificationToExpectation(testData)
        }

        testData.partners.zipWithIndex.flatMap { case (x, index) => toList(x, index) }
      }

      def test(testData: Partners) {
        implicit val cache =
          getCustomizedMap(partnerDetails = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(partnerDetails = true)

        val expectedHeading = tableHeaderForContainers(Messages("awrs.view_application.business_partners_text"), testData.partners)

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData)
      }

      test(testData)
    }

    "display additional premises correctly" in {
      implicit val divId = additionalPremisesId
      val testData = AdditionalBusinessPremisesList(
        List(testAdditionalBusinessPremises,
          testAdditionalBusinessPremises,
          testAdditionalBusinessPremises))

      def toExpectation(testData: AdditionalBusinessPremisesList): List[Row] = {
        def toList(testData: AdditionalBusinessPremises, id: Int): List[Row] =
          addressToExpectation((id + 1).toString + ".", testData.additionalAddress)

        testData.premises.zipWithIndex.flatMap { case (x, id) => toList(x, id) }
      }

      def test(testData: AdditionalBusinessPremisesList): Unit = {
        implicit val cache =
          getCustomizedMap(additionalBusinessPremises = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(additionalBusinessPremises = true)

        val expectedHeading = tableHeaderForContainers(Messages("awrs.view_application.additional_premises_text"), testData.premises)

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData)
      }

      test(testData)

    }

    "display business directors correctly" in {
      implicit val divId = businessDirectorsId

      def businessDirectorOneViewTest(directorsAndCompanySecretaries: Option[String] = "Director and Company Secretary",
                                      personOrCompany: Option[String] = "company",
                                      firstName: Option[String] = "John", lastName: Option[String] = "Smith",
                                      doTheyHaveNationalInsurance: Option[String] = "Yes", nino: Option[String] = testNino,
                                      passportNumber: Option[String] = None, nationalID: Option[String] = None,
                                      isDirectorACompany: Option[String] = None,
                                      companyName: Option[String] = "Company Name", tradingName: Option[String] = "Trading Name",
                                      doYouHaveVRN: Option[String] = "Yes", vrn: Option[String] = testVrn,
                                      doYouHaveCRN: Option[String] = "Yes", companyRegNumber: Option[String] = testCrn,
                                      doYouHaveUTR: Option[String] = "Yes", utr: Option[String] = testUtr,
                                      otherDirectors: Option[String] = "Yes"): BusinessDirector = {
        testBusinessDirectorDefault(
          personOrCompany = personOrCompany,
          firstName = firstName,
          lastName = lastName,
          doTheyHaveNationalInsurance = doTheyHaveNationalInsurance,
          nino = nino,
          passportNumber = passportNumber,
          nationalID = nationalID,
          businessName = companyName,
          tradingName = tradingName,
          doYouHaveUTR = doYouHaveUTR,
          utr = utr,
          doYouHaveCRN = doYouHaveCRN,
          companyRegNumber = companyRegNumber,
          doYouHaveVRN = doYouHaveVRN,
          vrn = vrn,
          directorsAndCompanySecretaries = directorsAndCompanySecretaries,
          otherDirectors = otherDirectors)
      }

      val testData = BusinessDirectors(List(
        businessDirectorOneViewTest(directorsAndCompanySecretaries = "Director and Company Secretary",
          personOrCompany = "company",
          companyName = None, tradingName = None, doYouHaveVRN = "No", vrn = None,
          doYouHaveCRN = "No", companyRegNumber = None, doYouHaveUTR = "No", utr = None),
        businessDirectorOneViewTest(firstName = None, lastName = None, tradingName = None, doTheyHaveNationalInsurance = "No", nino = None),
        businessDirectorOneViewTest(firstName = None, lastName = None, companyName = None, doTheyHaveNationalInsurance = "No", nino = None)
      ))

      val fetchName = (director: BusinessDirector) => {
        val businessName = director.companyNames.businessName
        val tradingName = director.companyNames.tradingName
        (director.firstName, director.lastName, businessName, tradingName) match {
          case (Some(fn), Some(ln), _, _) => fn + " " + ln
          case (_, _, Some(cn), _) => cn
          case (_, _, _, Some(tn)) => tn
          case _ => "" // this should never happen
        }
      }

      val fetchUtrHeading = (businessLegalEntity: String) => businessLegalEntity match {
        case "SOP" => Messages("awrs.generic.do_you_have_sa_UTR") // n.b. this case should never happen
        case "Partnership" | "LP" | "LLP" | "LLP_GRP" => Messages("awrs.generic.do_they_have_partnership_UTR")
        case _ => Messages("awrs.generic.do_they_have_CT_UTR")
      }

      def identificationToExpectation(businessLegalEntity: String, businessDirector: BusinessDirector): List[Row] =
        prepRow(fetchUtrHeading(businessLegalEntity), businessDirector.doYouHaveUTR) ++
          prepRow(Messages("awrs.generic.UTR_number"), businessDirector.utr) ++
          prepRow(Messages("awrs.generic.do_they_have_NINO"), businessDirector.doTheyHaveNationalInsurance) ++
          prepRow(Messages("awrs.generic.NINO"), businessDirector.nino) ++
          prepRow(Messages("awrs.generic.do_they_have_company_reg"), businessDirector.doYouHaveCRN) ++
          prepRow(Messages("awrs.generic.company_reg"), businessDirector.companyRegNumber) ++
          prepRow(Messages("awrs.generic.do_they_have_VAT"), businessDirector.doYouHaveVRN) ++
          prepRow(Messages("awrs.generic.VAT_registration_number"), businessDirector.vrn)

      def toExpectation(businessLegalEntity: String, testData: BusinessDirectors): List[Row] = {
        def toList(testData: BusinessDirector): List[Row] =
          Row(fetchName(testData), None) ++
            prepRow(Messages("awrs.generic.trading_name"), testData.companyNames.tradingName) ++
            prepRow(Messages("awrs.business_directors.role_question.additional"), DirectorAndSecretaryEnum.getMessageKey(testData.directorsAndCompanySecretaries.get)) ++
            prepRow(Messages("awrs.business_directors.personOrCompany_question"), PersonOrCompanyEnum.getMessageKey(testData.personOrCompany.get)) ++
            identificationToExpectation(businessLegalEntity, testData)

        testData.directors.flatMap(x => toList(x))
      }

      def test(entity: BusinessType, testData: BusinessDirectors): Unit = {
        implicit val cache =
          getCustomizedMap(
            businessDirectors = testData,
            businessType = entity)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(businessDirectors = true)

        val expectedHeading = tableHeaderForContainers(Messages("awrs.view_application.business_directors.index_text"), testData.directors)

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(entity.legalEntity.fold("")(x => x), testData)
      }

      val entities = Seq(testBusinessDetailsEntityTypes(Partnership),
        testBusinessDetailsEntityTypes(Lp),
        testBusinessDetailsEntityTypes(Llp),
        testBusinessDetailsEntityTypes(GroupRep),
        testBusinessDetailsEntityTypes(CorporateBody))
      entities.foreach(entity => test(entity, testData))

    }

    "display trading activity correctly" in {
      implicit val divId = tradingActivityId
      val testData = testTradingActivity()

      def toExpectation(testData: TradingActivity): List[Row] =
        Row("", None) ++
          multipleChoicesToExpectation(Messages("awrs.additional_information.wholesaler_type"), testData.wholesalerType, testData.otherWholesaler)(AwrsFormFields.wholesaler.toMap) ++
          multipleChoicesToExpectation(Messages("awrs.additional_information.orders.orders_question"), testData.typeOfAlcoholOrders, testData.otherTypeOfAlcoholOrders)(AwrsFormFields.orders.toMap) ++
          prepRow(Messages("awrs.additional_information.alcohol_import"), testData.doesBusinessImportAlcohol) ++
          prepRow(Messages("awrs.additional_information.alcohol_export"), testData.doYouExportAlcohol) ++
          optMultipleChoicesToExpectation(Messages("awrs.additional_information.export_location"), testData.exportLocation)(AwrsFormFields.exportAlcohol.toMap) ++
          prepRow(Messages("awrs.additional_information.third_party_storage"), testData.thirdPartyStorage)


      def test(testData: TradingActivity): Unit = {
        implicit val cache =
          getCustomizedMap(tradingActivity = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(tradingActivity = true)

        val expectedHeading = Messages("awrs.view_application.trading_activity_text")

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData).drop(1)
      }

      test(testData)
    }

    "display products correctly" in {
      implicit val divId = productsId
      val testData = testProducts()

      def toExpectation(testData: Products): List[Row] =
        Row("", None) ++
          multipleChoicesToExpectation(Messages("awrs.additional_information.main_customers"), testData.mainCustomers, testData.otherMainCustomers)(AwrsFormFields.mainCustomerOptions.toMap) ++
          multipleChoicesToExpectation(Messages("awrs.additional_information.products"), testData.productType, testData.otherProductType)(AwrsFormFields.products.toMap)


      def test(testData: Products): Unit = {
        implicit val cache =
          getCustomizedMap(products = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(products = true)

        val expectedHeading = Messages("awrs.view_application.products_text")

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData).drop(1)
      }

      test(testData)
    }

    "display suppliers correctly" in {
      when(mockCountryCodes.getCountry(ArgumentMatchers.any()))
        .thenReturn(None, Some("United States of America"))

      implicit val divId = suppliersId
      // Data in testUtil is not correct
      val testData = Suppliers(List(
        Supplier(
          alcoholSuppliers = Some("Yes"),
          supplierName = Some("Smith and co"),
          vatRegistered = Some("yes"),
          vatNumber = testVrn,
          supplierAddress = Some(Address("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("NE12 2DS"), Some("GB"))),
          additionalSupplier = Some("Yes"),
          ukSupplier = Some("Yes")),
        Supplier(
          alcoholSuppliers = Some("Yes"),
          supplierName = Some("any"),
          vatRegistered = Some("No"),
          vatNumber = None,
          supplierAddress = Some(Address(postcode = None, addressLine1 = "Line 1", addressLine2 = "Line 2", addressLine3 = Some("Line 3"), addressLine4 = Some("Line 4"), addressCountryCode = Some("US"), addressCountry = Some("United States of America"))),
          additionalSupplier = Some("No"),
          ukSupplier = Some("No"))
      ))


      def supplierAddressToExpectation(heading: String, testAddress: Option[Address], ukSupplier: Option[String]): List[Row] = {
        def prepList(someStr: Option[String]): List[String] = someStr match {
          case Some(str) => List(str)
          case _ => List()
        }

        testAddress match {
          case Some(address) =>
            Row(heading,
              List(address.addressLine1,
                address.addressLine2) ++
                prepList(address.addressLine3) ++
                prepList(address.addressLine4) ++
                address.postcode ++
                (ukSupplier match {
                  case Some("Yes") => List()
                  case _ => address.addressCountry
                })
            )
          case _ => List()
        }
      }

      def toExpectation(testData: Suppliers): List[Row] = {
        def toList(testData: Supplier): List[Row] =
          Row(testData.supplierName.get, None) ++
            prepRow(Messages("awrs.supplier-addresses.uk_supplier"), testData.ukSupplier) ++
            supplierAddressToExpectation(Messages("awrs.generic.address"), testData.supplierAddress, testData.ukSupplier) ++
            prepRow(Messages("awrs.supplier-addresses.vat_registered"), testData.vatRegistered) ++
            prepRow(Messages("awrs.generic.VAT_registration_number"), testData.vatNumber)

        testData.suppliers.flatMap(x => toList(x))
      }

      def test(testData: Suppliers): Unit = {
        implicit val cache =
          getCustomizedMap(suppliers = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(suppliers = true)
        val expectedHeading = tableHeaderForContainers(Messages("awrs.view_application.suppliers_text"), testData.suppliers)

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData)
      }

      test(testData)

    }

    "display aplication declaration correctly" in {
      implicit val divId = applicationDeclarationId
      val testData = testApplicationDeclaration

      def toExpectation(testData: ApplicationDeclaration): List[Row] =
        prepRow(Messages("awrs.application_declaration.declaration_name_hint", "sending"), testData.declarationName) ++
          prepRow(Messages("awrs.application_declaration.declaration_role_hint", "sending"), testData.declarationRole)

      def test(testData: ApplicationDeclaration): Unit = {
        implicit val cache =
          getCustomizedMap(applicationDeclaration = testData)

        implicit val doc = getDoc()
        val subview = getSubview

        testSectionExists(applicationDeclaration = true)

        val expectedHeading = Messages("awrs.view_application.application_declaration_text")

        subview.heading shouldBe expectedHeading
        subview.rows shouldBe toExpectation(testData)
      }

      test(testData)
    }

  }

  def testSectionExists(businessDetails: Boolean = false,
                        businessRegistrationDetails: Boolean = false,
                        placeOfBusiness: Boolean = false,
                        businessContacts: Boolean = false,
                        groupMembers: Boolean = false,
                        partnerDetails: Boolean = false,
                        additionalBusinessPremises: Boolean = false,
                        businessDirectors: Boolean = false,
                        tradingActivity: Boolean = false,
                        products: Boolean = false,
                        suppliers: Boolean = false,
                        applicationDeclaration: Boolean = false
                       )(implicit doc: Document) = {

    doc.getElementById(businessDetailsId) != null shouldBe businessDetails
    doc.getElementById(businessRegistrationDetailsId) != null shouldBe businessRegistrationDetails
    doc.getElementById(placeOfBusinessId) != null shouldBe placeOfBusiness
    doc.getElementById(businessContactsId) != null shouldBe businessContacts
    doc.getElementById(groupMemberDetailsId) != null shouldBe groupMembers
    doc.getElementById(partnerDetailsId) != null shouldBe partnerDetails
    doc.getElementById(additionalPremisesId) != null shouldBe additionalBusinessPremises
    doc.getElementById(businessDirectorsId) != null shouldBe businessDirectors
    doc.getElementById(tradingActivityId) != null shouldBe tradingActivity
    doc.getElementById(productsId) != null shouldBe products
    doc.getElementById(suppliersId) != null shouldBe suppliers
    doc.getElementById(applicationDeclarationId) != null shouldBe applicationDeclaration
  }

  case class Row(heading: String, content: List[String])

  case class Subview(heading: String, rows: List[Row])

  def getRow(tr: Element): Row = {
    val row1stCol = tr.getElementsByTag("th")
    val row = tr.getElementsByTag("td")
    // there are 2 tds (heading th | content td), where content may have multiple paragraphs
    Row(row.head.text, row.drop(1).head.getElementsByTag("p").foldLeft(List[String]()){ (list, x) =>
      list :+ x.text
    })
  }

  def getDoc(entityType: String = "SOP")(implicit cache: CacheMap) = {
    val result = testController.show(cache)(request.withSession(AwrsSessionKeys.sessionBusinessType -> entityType))
    Jsoup.parse(contentAsString(result))
  }

  def getSubview(implicit doc: Document, id: String): Subview = {
    val div = doc.getElementById(id)
    val heading = div.getElementsByTag("h1").head.text()
    val rows = div.getElementsByTag("tr").foldLeft(List[Row]())((list, x) => list :+ getRow(x))
    Subview(heading, rows)
  }

}
