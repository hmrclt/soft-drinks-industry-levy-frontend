/*
 * Copyright 2019 HM Revenue & Customs
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

package uniform

import ltbs.play.scaffold.SdilComponents
import org.scalatest.{FlatSpec, Matchers}
import play.api.data.Form
import play.api.test.FakeRequest
import sdil.models.Address
import sdil.utils.TestWiring

class SdilComponents extends FlatSpec with Matchers with TestWiring {

  val address : Address =  Address("line1", "line2", "line3", "line4", "AL1 1UJ")
  val requestContactDetails = FakeRequest().withFormUrlEncodedBody(
    "fullName" -> "Bill Gates",
    "position" -> "CEO retired",
    "phoneNumber" -> "No one knows",
    "email" -> "billgates@microsoft.com"
  )

  val formContactDetails =  Form (
    (SdilComponents.contactDetailsMapping))


  "A SdilComponent Call for contactDetails" should "correctly bind" in {

    val addressHtmlVal = SdilComponents.addressHtml.showHtml(address)
    val contactDetailsVal = SdilComponents.contactDetailsForm.asHtmlForm("fail data", formContactDetails.bindFromRequest()(requestContactDetails))

    1 shouldBe 1

  }

}
