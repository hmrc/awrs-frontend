package forms

import forms.AwrsEnrollmentUrnForm.{awrsEnrolmentUrnForm, awrsUrn, maxQueryLength}
import forms.test.util.NamedUnitTests.{assertFormIsValid, singleFieldTestFunctions}
import forms.test.util.{CompulsoryFieldValidationExpectations, ExpectedFieldExceedsMaxLength, ExpectedFieldFormat, ExpectedFieldIsEmpty, ExpectedInvalidFieldFormat}
import forms.validation.util.{FieldError, MessageArguments, SummaryError}
import models.AwrsEnrollmentUrn
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form

class AwrsEnrolmentUrnFormSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  "AwrsEnrolmentUrnForm" should {
    implicit val form: Form[AwrsEnrollmentUrn] = awrsEnrolmentUrnForm.form

    "validate and generate the correct error messages" in {
      val fieldId: String = awrsUrn
      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.search.query.empty"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "urn field", maxQueryLength)
      val summaryError = (message: String) => SummaryError(message, MessageArguments("urn field"), fieldId)
      val invalidFormats = List(
        ExpectedInvalidFieldFormat("α", fieldId, "search field"),
        ExpectedInvalidFieldFormat("XAAW000001234567", FieldError("awrs.search.query.string_length_mismatch"), summaryError("awrs.search.query.string_length_mismatch")),
        ExpectedInvalidFieldFormat("XAAW0000012345", FieldError("awrs.search.query.string_length_mismatch"), summaryError("awrs.search.query.string_length_mismatch")),
        ExpectedInvalidFieldFormat("XAAW00001123456", FieldError("awrs.search.query.zeros_mismatch"), summaryError("awrs.search.query.zeros_mismatch")),
        ExpectedInvalidFieldFormat("XAAW0000012345X", FieldError("awrs.search.query.default_invalid_urn"), summaryError("awrs.search.query.default_invalid_urn")),
        ExpectedInvalidFieldFormat("X0AW00000123456", FieldError("awrs.search.query.default_invalid_urn"), summaryError("awrs.search.query.default_invalid_urn")),
        ExpectedInvalidFieldFormat("XXA000000123456", FieldError("awrs.search.query.default_invalid_urn"), summaryError("awrs.search.query.default_invalid_urn")),
        //when name search is reinstated delete line below
        ExpectedInvalidFieldFormat("Xy company 188555", FieldError("awrs.search.query.default_invalid_urn"), summaryError("awrs.search.query.default_invalid_urn"))

      )
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

      fieldId assertFieldIsCompulsory expectations
    }

    "allow valid submissions" in {
      assertFormIsValid(form, Map(awrsUrn -> "XAAW00000123456"))
      assertFormIsValid(form, Map(awrsUrn -> "XSAW00000123456"))
      assertFormIsValid(form, Map(awrsUrn -> "XZAW00000999999"))
      assertFormIsValid(form, Map(awrsUrn -> "XFAW00000000000"))
    }

  }


}
