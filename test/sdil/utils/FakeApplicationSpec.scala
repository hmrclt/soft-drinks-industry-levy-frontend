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

package sdil.utils

import java.io.File

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.defaultContext
import play.api.test.FakeRequest
import play.api.{Application, ApplicationLoader, Configuration, Environment}
import play.core.DefaultWebCommands
import sdil.config.{ConnectorWiring, RegistrationFormDataCache, SDILApplicationLoader}
import sdil.connectors.SoftDrinksIndustryLevyConnector
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

/*
trait FakeApplicationSpec extends PlaySpec with BaseOneAppPerSuite with FakeApplicationFactory with TestWiring {
  override def fakeApplication: Application = {
    val context = ApplicationLoader.Context(
      environment = env,
      sourceMapper = None,
      webCommands = new DefaultWebCommands,
      initialConfiguration = configuration
    )
    val loader = new SDILApplicationLoader
    loader.load(context)
  }
}
*/


trait FakeApplicationSpec extends PlaySpec with WithFakeApplication with MockitoSugar with GenericAppConfig {

  lazy val envTest: Environment = Environment.simple()
  val configuration = Configuration.load(envTest)


  val messagesApi: MessagesApi = new DefaultMessagesApi(envTest, runModeConfiguration, new DefaultLangs(runModeConfiguration))
  implicit val defaultMessages: Messages = messagesApi.preferred(FakeRequest())
  implicit val ec: ExecutionContext = defaultContext

  type Retrieval = Enrolments ~ Option[CredentialRole] ~ Option[String] ~ Option[AffinityGroup]

  lazy val mockAuthConnector: AuthConnector = {
    val m = mock[AuthConnector]
    when(m.authorise[Retrieval](any(), any())(any(), any())).thenReturn {
      Future.successful(new ~(new ~(new ~(Enrolments(Set.empty), Some(Admin)), Some("internal id")), Some(Organisation)))
    }
    m
  }

  lazy val mockSdilConnector: SoftDrinksIndustryLevyConnector = {
    val m = mock[SoftDrinksIndustryLevyConnector]
    when(m.submit(any(),any())(any())).thenReturn(Future.successful(()))
    when(m.retrieveSubscription(any(),any())(any())).thenReturn(Future.successful(None))
    m
  }

  val mockCache: RegistrationFormDataCache = {
    val m = mock[RegistrationFormDataCache]
    when(m.cache(anyString(), any())(any())).thenReturn(Future.successful(CacheMap("", Map.empty)))
    when(m.get(anyString())(any())).thenReturn(Future.successful(None))
    when(m.clear(anyString())(any())).thenReturn(Future.successful(()))
    m
  }
}