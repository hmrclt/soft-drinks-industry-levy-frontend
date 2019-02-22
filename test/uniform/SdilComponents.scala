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

import java.time.LocalDate

import ltbs.play.scaffold.SdilComponents
import org.scalatest.{FlatSpec, Matchers}
import play.api.data.Form
import play.api.test.FakeRequest
import sdil.models.Address
import sdil.models.backend.{Site, UkAddress}
import sdil.utils.TestWiring

class SdilComponents extends FlatSpec with Matchers with TestWiring {

  val address : Address =  Address("line1", "line2", "line3", "line4", "AL1 1UJ")
  val uKAddress : UkAddress = UkAddress(List("ukAddr1", "ukAddr2"), "WD25 7HQ")
  val requestContactDetails = FakeRequest().withFormUrlEncodedBody(
    "fullName" -> "Bill Gates",
    "position" -> "CEO retired",
    "phoneNumber" -> "No one knows",
    "email" -> "billgates@microsoft.com"
  )

  val requestSiteDetails = FakeRequest().withFormUrlEncodedBody(
    "address" -> uKAddress.toString,
    "position" -> "CEO retired",
    "phoneNumber" -> "No one knows",
    "email" -> "billgates@microsoft.com"
  )


  val formContactDetails =  Form (SdilComponents.contactDetailsMapping)
  val wareHouseSiteMapping = Form(SdilComponents.warehouseSiteMapping)

  "A SdilComponent Call for contactDetails" should "correctly bind" in {

    val addressHtmlVal = SdilComponents.addressHtml.showHtml(address)
    val contactDetailsVal = SdilComponents.contactDetailsForm.asHtmlForm("fail data", formContactDetails.bindFromRequest()(requestContactDetails))

    val dateMap = Map("day" ->  "01",
    "month" -> "02",
    "year" -> "2019")

    val siteObj = Site(uKAddress, Some("refOption"), Some("Arcade tradingName"), Some(LocalDate.now()))

    val dayMissingMap = dateMap  + ("day" -> "")
    val monthMissingMap = dateMap  + ("month" -> "")
    val yearMissingMap = dateMap  + ("year" -> "")
    val dayAndMonthMissingMap = dateMap  + ("day" -> "") + ("month" -> "")
    val dayAndYearMissingMap = dateMap  + ("day" -> "") + ("year" -> "")
    val monthAndYearMissingMap = dateMap  + ("month" -> "")  + ("year" -> "")
    val allMissingMap = dateMap  + ("day" -> "") + ("month" -> "")  + ("year" -> "")
    val invalidDayMap = dateMap  + ("day" -> "567")


    SdilComponents.startDate.bind(dateMap).toString
    SdilComponents.startDate.bind(dayMissingMap)
    SdilComponents.startDate.bind(monthMissingMap)
    SdilComponents.startDate.bind(yearMissingMap)
    SdilComponents.startDate.bind(dayAndMonthMissingMap)
    SdilComponents.startDate.bind(dayAndYearMissingMap)
    SdilComponents.startDate.bind(monthAndYearMissingMap)
    SdilComponents.startDate.bind(allMissingMap)
    SdilComponents.startDate.bind(invalidDayMap)

    SdilComponents.numeric("001")
    SdilComponents.numeric("NotNumberic").bind(Map("dfgdf" -> "dfvdfv"))
    SdilComponents.numeric("22.345")

    SdilComponents.siteProgressiveRevealHtml.showHtml(siteObj).body


    1 shouldBe 1

  }

}
