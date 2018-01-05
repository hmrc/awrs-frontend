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

package controllers

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import controllers.util._
import forms.AWRSEnums.BooleanRadioEnum
import forms.SupplierAddressesForm._
import models.{Supplier, Suppliers}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc._
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.CountryCodes
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait SupplierAddressesController extends AwrsController
  with JourneyPage
  with Deletable[Suppliers, Supplier] with SaveAndRoutable {

  override def fetch(implicit user: AuthContext, hc: HeaderCarrier) = save4LaterService.mainStore.fetchSuppliers

  override def save(data: Suppliers)(implicit user: AuthContext, hc: HeaderCarrier) = save4LaterService.mainStore.saveSuppliers(data)

  override val listToListObj = (suppliers: List[Supplier]) => Suppliers(suppliers)
  override val listObjToList = (suppliers: Suppliers) => suppliers.suppliers
  override val backCall = controllers.routes.ViewApplicationController.viewSection(suppliersName)
  override val section: String = suppliersName
  override val deleteFormAction: Int => Call = (id: Int) => controllers.routes.SupplierAddressesController.actionDelete(id)
  override lazy val deleteHeadingParameter: String = Messages("awrs.view_application.supplier")
  override val addNoAnswerRecord = (emptyList: List[models.Supplier]) => List(Supplier(Some("No"), None, None, None, None, None, None))
  override val amendHaveAnotherAnswer = (data: Supplier, newAnswer: String) => data.copy(additionalSupplier = Some(newAnswer))

  val maxSuppliers = 5

  def showSupplierAddressesPage(id: Int, isLinearMode: Boolean, isNewRecord: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      lazy val newEntryAction = (id: Int) =>
        Future.successful(Ok(views.html.awrs_supplier_addresses(supplierAddressesForm.form, id, isNewRecord)))

      lazy val existingEntryAction = (data: Suppliers, id: Int) => {
        val supplier = data.suppliers(id - 1)
        val updatedSupplier = supplier.copy(supplierAddress = CountryCodes.getSupplierAddressWithCountry(supplier))
        Future.successful(Ok(views.html.awrs_supplier_addresses(supplierAddressesForm.form.fill(updatedSupplier), id, isNewRecord)))
      }

      lazy val haveAnother = (data: Suppliers) =>
        data.suppliers.last.additionalSupplier.fold("")(x => x).equals(BooleanRadioEnum.YesString)

      lookup[Suppliers, Supplier](
        fetchData = fetch,
        id = id,
        toList = (suppliers: Suppliers) => suppliers.suppliers,
        maxEntries = Some(maxSuppliers)
      )(
        newEntryAction = newEntryAction,
        existingEntryAction = existingEntryAction,
        haveAnother = haveAnother
      )
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    supplierAddressesForm.bindFromRequest.fold(
      formWithErrors =>
        fetch flatMap {
          case Some(data) => Future.successful(BadRequest(views.html.awrs_supplier_addresses(formWithErrors, id, isNewRecord)))
          case _ => Future.successful(BadRequest(views.html.awrs_supplier_addresses(formWithErrors, 1, isNewRecord)))
        },
      supplierAddressesData => {
        val countryCodeSupplierAddressData = supplierAddressesData.copy(supplierAddress = CountryCodes.getSupplierAddressWithCountryCode(supplierAddressesData))
        countryCodeSupplierAddressData.alcoholSuppliers match {
          case Some("No") =>
            save(Suppliers(suppliers = List(countryCodeSupplierAddressData))) flatMap {
              _ => Future.successful(Redirect(controllers.routes.IndexController.showIndex())) // No suppliers so return to index
            }
          case _ =>

            saveThenRedirect[Suppliers, Supplier](
              fetchData = fetch,
              saveData = save,
              id = id,
              data = countryCodeSupplierAddressData
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

object SupplierAddressesController extends SupplierAddressesController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
