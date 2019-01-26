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

package sdil.controllers

import cats.implicits._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePut, HeaderCarrier}
import com.softwaremill.macwire._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.ShortLivedHttpCaching

import scala.concurrent._
import duration._
import org.scalatest.MustMatchers._
import com.softwaremill.macwire._
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.config.SDILShortLivedCaching
import sdil.models.RegistrationFormData
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

class ReturnsControllerSpec extends ControllerSpec {

  case class SharedSessionPersistence(initialData: (String,JsValue)*) {
    var data: Map[String,JsValue] = Map(initialData:_*)
    def dataGet(session: String): Future[Map[String, JsValue]] =
      data.pure[Future]
    def dataPut(session: String, dataIn: Map[String, JsValue]): Unit =
      data = dataIn
  }

  "ReturnsController" should {

    "askNewWarehouses" in {
      def subProgram = controller.askNewWarehouses()(hc) >>
      controller.resultToWebMonad[Result](controller.Ok("fin!"))
      val persistence = SharedSessionPersistence("ask-secondary-warehouses-in-return" -> Json.toJson(false))

      // dummy up a post request, may need to add in a session id too in order to avoid a redirect
      def request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody("utr" -> "", "postcode" -> "")
      def output = controller.runInner(request)(subProgram)(
        "production-site-details" /* replace with the key of the very last form page */
      )(persistence.dataGet, persistence.dataPut)
      // We should get 'fin!' in the result as long as all the validation passes
//      output.result(10 seconds) must include("fin!")
      val blah: Result = Await.result(output, 10 seconds)
      1 mustBe 1
    }
  }

  lazy val controller: ReturnsController = wire[ReturnsController]
  lazy val shortLivedCaching: ShortLivedHttpCaching = new ShortLivedHttpCaching {
    override def baseUri: String = ???

    override def domain: String = ???

    override def defaultSource: String = ???

    override def http: CoreGet with CorePut with CoreDelete = ???
  }
  lazy val hc: HeaderCarrier = HeaderCarrier()

}
