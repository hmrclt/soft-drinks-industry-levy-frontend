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

package sdil.config

import com.google.inject.AbstractModule
import javax.inject.Inject
import play.api.{Configuration, Environment, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.play.config.RunMode

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule{
  override def configure(): Unit = {
    bind(classOf[RunMode]).to(classOf[DefaultRunMode])
  }
}

class DefaultRunMode @Inject() (val runModeConfiguration: Configuration, environment: Environment) extends RunMode {
  override protected def mode: Mode = environment.mode
}