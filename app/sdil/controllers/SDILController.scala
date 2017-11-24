/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Inject

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{text, email, mapping}
import play.api.i18n.Messages
import play.api.mvc._
import sdil.config.FrontendAppConfig._
import sdil.config.{FormDataCache, FrontendAuthConnector}
import sdil.connectors.SoftDrinksIndustryLevyConnector
import sdil.models._
import sdil.models.sdilmodels._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.softdrinksindustrylevy._

import scala.concurrent.Future

class SDILController @Inject() (
  val messagesApi: play.api.i18n.MessagesApi,
  sdilConnector: SoftDrinksIndustryLevyConnector) extends AuthorisedFunctions with FrontendController
  with play.api.i18n.I18nSupport {

  override def authConnector: AuthConnector = FrontendAuthConnector

  val cache: SessionCache = FormDataCache

  private def authorisedForSDIL(action: Request[AnyContent] => String => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(saUtr) {
        case Some(utr) => action(request)(utr)
        case _ => Future successful Redirect(ggLoginUrl, Map("continue" -> Seq(sdilHomePage), "origin" -> Seq(appName)))
      } recover {
        case e: NoActiveSession =>
          Logger.warn(s"Bad person $e")
          Redirect(ggLoginUrl, Map("continue" -> Seq(sdilHomePage), "origin" -> Seq(appName)))
      }
    }
  }

  def displayContactDetails: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok(register.contact_details(contactForm))
  }

  def submitContactDetails: Action[AnyContent] = Action.async { implicit request =>
    contactForm.bindFromRequest().fold(
      formWithErrors => Future successful BadRequest(register.contact_details(formWithErrors)),
      d => cache.cache("contact-details", d) map { _ =>
        Redirect(routes.SDILController.displayDeclaration())
      })
  }

  def displayDeclaration = TODO

  def testAuth: Action[AnyContent] = authorisedForSDIL { implicit request => implicit utr =>
    Future successful Ok(views.html.helloworld.hello_world(Some(DesSubmissionResult(true))))
  }

  private val contactForm = Form(
    mapping(
      "fullName" -> text.verifying(Messages("error.full-name.invalid"), _.nonEmpty),
      "position" -> text.verifying(Messages("error.position.invalid"), _.nonEmpty),
      "phoneNumber" -> text.verifying(Messages("error.phone-number.invalid"), _.length > 10),
      "email" -> email)(ContactDetails.apply)(ContactDetails.unapply))

}