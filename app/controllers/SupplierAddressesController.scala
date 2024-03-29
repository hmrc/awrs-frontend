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

package controllers

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.SupplierAddressesForm._
import models.{Supplier, Suppliers}
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, CountryCodes}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierAddressesController @Inject()(val mcc: MessagesControllerComponents,
                                            val save4LaterService: Save4LaterService,
                                            val deEnrolService: DeEnrolService,
                                            val authConnector: DefaultAuthConnector,
                                            val auditable: Auditable,
                                            val accountUtils: AccountUtils,
                                            val countryCodes: CountryCodes,
                                            implicit val applicationConfig: ApplicationConfig,
                                            template: views.html.awrs_supplier_addresses) extends FrontendController(mcc) with AwrsController
  with JourneyPage
  with Deletable[Suppliers, Supplier] with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn
  val countryList: Seq[(String, String)] = countryCodes.countryCodesTuplesSeq

  override def fetch(authRetrievals: StandardAuthRetrievals)
                    (implicit hc: HeaderCarrier): Future[Option[Suppliers]] = save4LaterService.mainStore.fetchSuppliers(authRetrievals)

  override def save(authRetrievals: StandardAuthRetrievals, data: Suppliers)(implicit hc: HeaderCarrier): Future[Suppliers]
  = save4LaterService.mainStore.saveSuppliers(authRetrievals, data)

  override val listToListObj: List[Supplier] => Suppliers = (suppliers: List[Supplier]) => Suppliers(suppliers)
  override val listObjToList: Suppliers => List[Supplier] = (suppliers: Suppliers) => suppliers.suppliers
  override lazy val backCall: Call = controllers.routes.ViewApplicationController.viewSection(suppliersName)
  override val section: String = suppliersName
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.SupplierAddressesController.actionDelete(id)
  override lazy val deleteHeadingParameter: String = "awrs.view_application.supplier"
  override val addNoAnswerRecord: List[Supplier] => List[Supplier] = (_: List[models.Supplier]) => List(Supplier(Some("No"), None, None, None, None, None, None))
  override val amendHaveAnotherAnswer: (Supplier, String) => Supplier = (data: Supplier, newAnswer: String) => data.copy(additionalSupplier = Some(newAnswer))

  def showSupplierAddressesPage(id: Int, isLinearMode: Boolean, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        lazy val newEntryAction = (id: Int) =>
          Future.successful(Ok(template(supplierAddressesForm.form, id, isNewRecord, countryList)))

        lazy val existingEntryAction = (data: Suppliers, id: Int) => {
          val supplier = data.suppliers(id - 1)
          val updatedSupplier = supplier.copy(supplierAddress = applicationConfig.countryCodes.getSupplierAddressWithCountry(supplier))
          Future.successful(Ok(template(supplierAddressesForm.form.fill(updatedSupplier), id, isNewRecord, countryList)))
        }

        lazy val haveAnother = (data: Suppliers) =>
          data.suppliers.last.additionalSupplier.fold("")(x => x).equals(BooleanRadioEnum.YesString)

        lookup[Suppliers, Supplier](
          fetchData = fetch(ar),
          id = id,
          toList = (suppliers: Suppliers) => suppliers.suppliers,
          maxEntries = Some(5)
        )(
          newEntryAction = newEntryAction,
          existingEntryAction = existingEntryAction,
          haveAnother = haveAnother
        )
      }
    }
  }

  private[controllers] def processCountryNonUK(s: Supplier): Supplier = {
    if (s.ukSupplier.contains("Yes")) {
      s.copy(supplierAddress = s.supplierAddress.map(_.copy(addressCountry = None, addressCountryCode = None)))
    } else {
      s
    }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    supplierAddressesForm.bindFromRequest().fold(
      formWithErrors =>
        fetch(authRetrievals) flatMap {
          case Some(_) => Future.successful(BadRequest(template(formWithErrors, id, isNewRecord, countryList)))
          case _ => Future.successful(BadRequest(template(formWithErrors, 1, isNewRecord, countryList)))
        },
      supplierAddressesData => {
        val countryValidatedSupplier = processCountryNonUK(supplierAddressesData)
        val countryCodeSupplierAddressData = countryValidatedSupplier.copy(supplierAddress = applicationConfig.countryCodes.getSupplierAddressWithCountryCode(countryValidatedSupplier))
        countryCodeSupplierAddressData.alcoholSuppliers match {
          case Some("No") =>
            save(authRetrievals, Suppliers(suppliers = List(countryCodeSupplierAddressData))) flatMap {
              _ => Future.successful(Redirect(controllers.routes.IndexController.showIndex)) // No suppliers so return to index
            }
          case _ =>

            saveThenRedirect[Suppliers, Supplier](
              fetchData = fetch(authRetrievals),
              saveData = save,
              id = id,
              data = countryCodeSupplierAddressData,
              authRetrievals = authRetrievals
            )(
              haveAnotherAnswer = (data: Supplier) => data.additionalSupplier.fold("")(x => x),
              amendHaveAnotherAnswer = amendHaveAnotherAnswer,
              hasSingleNoAnswer = (fetchData: Suppliers) => fetchData.suppliers.head.alcoholSuppliers.fold("")(x => x)
            )(
              listObjToList = (listObj: Suppliers) => listObj.suppliers,
              listToListObj = (list: List[Supplier]) => Suppliers(list)
            )(
              redirectRoute = (answer: String, id: Int) => redirectRoute(Some(RedirectParam(answer, id)), isNewRecord)
            )
        }
      }
    )

  }
}
