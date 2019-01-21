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

import com.softwaremill.macwire._
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.models.RegistrationFormData
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future
import play.api.mvc._
import sdil.uniform._
import uk.gov.hmrc.http.HeaderCarrier
import cats.implicits._
import uk.gov.hmrc.uniform.webmonad._
import uk.gov.hmrc.uniform._
import play.api.libs.json._

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
      val persistence = new SharedSessionPersistence(/* serialise the answers here */ )

      // dummy up a post request, may need to add in a session id too in order to avoid a redirect
      def request: Request[AnyContent] = ??? 
      def output = controller.runInner(request)(subProgram)(
        "lastPage" /* replace with the key of the very last form page */
      )(persistence.dataGet, persistence.dataPut)
      // We should get 'fin!' in the result as long as all the validation passes
    }
  }

  lazy val controller: ReturnsController = ??? //wire[ReturnsController]
  lazy val hc: HeaderCarrier = ???

}
