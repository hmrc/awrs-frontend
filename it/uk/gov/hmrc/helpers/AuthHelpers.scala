
package uk.gov.hmrc.helpers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers.OK
import uk.gov.hmrc.helpers.http.StubbedBasicHttpCalls

trait AuthHelpers extends StubbedBasicHttpCalls {

  def mockAuthedCall: StubMapping = {
    stubbedPost("/write/audit", OK, """{"x":2}""")
    stubbedPost("/write/audit/merged", OK, """{"x":2}""")
    stubbedPost("/auth/authorise", OK, """{}""")
  }

}
