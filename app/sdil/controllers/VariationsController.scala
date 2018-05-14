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

package sdil.controllers

import cats.implicits._
import enumeratum._
import java.time.LocalDate
import ltbs.play._
import ltbs.play.scaffold._
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html
import scala.concurrent.{ExecutionContext, Future}
import sdil.actions.RegisteredAction
import sdil.config._
import sdil.connectors.SoftDrinksIndustryLevyConnector
import sdil.models._
import sdil.models.backend.{ Contact, Site, UkAddress }
import sdil.models.retrieved.{ RetrievedActivity, RetrievedSubscription }
import sdil.models.variations._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.gdspages
import webmonad._

class VariationsController(
  val messagesApi: MessagesApi,
  sdilConnector: SoftDrinksIndustryLevyConnector,
  registeredAction: RegisteredAction,
  keystore: SessionCache
)(implicit
  val config: AppConfig,
  val ec: ExecutionContext
) extends SdilWMController with FrontendController with GdsComponents with SdilComponents {

  sealed trait ChangeType extends EnumEntry
  object ChangeType extends Enum[ChangeType] {
    val values = findValues
    case object Sites extends ChangeType
    case object Activity extends ChangeType
    case object Deregister extends ChangeType
  }

  private def contactUpdate(
    data: VariationData
  ): WebMonad[VariationData] = for {
    contact         <- ask(contactDetailsMapping, "contact", data.updatedContactDetails)
    warehouses      <- manyT("warehouses", askSite(_), default = data.updatedWarehouseSites.toList)
    packSites       <- manyT("packSites", askSite(_), default = data.updatedProductionSites.toList)
    businessAddress <- ask(addressMapping, "businessAddress", default = data.updatedBusinessAddress)
  } yield data.copy (
    updatedBusinessAddress = businessAddress,
    updatedProductionSites = packSites,
    updatedWarehouseSites  = warehouses,
    updatedContactDetails  = contact
  )

  implicit def optFormatter[A](implicit innerFormatter: Format[A]): Format[Option[A]] =
    new Format[Option[A]] {
      def reads(json: JsValue): JsResult[Option[A]] = json match {
        case JsNull => JsSuccess(none[A])
        case a      => innerFormatter.reads(a).map{_.some}
      }
      def writes(o: Option[A]): JsValue =
        o.map{innerFormatter.writes}.getOrElse(JsNull)
      
    }

  private def activityUpdate(
    data: VariationData
  ): WebMonad[VariationData] = for {
    //packLarge       <- ask(bool, "packLarge", data.producer.isLarge) when ask(bool, "package", data.producer.isProducer)
    packLarge       <- ask[Option[Boolean]](innerOpt(bool), "packLarge")
    packQty         <- ask(litreagePair.nonEmpty, "packQty") when ask(bool, "packOpt", data.updatedProductionSites.nonEmpty)
    useCopacker     <- ask(bool,"useCopacker", data.usesCopacker)
    copacks         <- ask(litreagePair.nonEmpty, "copackQty") when ask(bool, "copacker", data.copackForOthers)
    imports         <- ask(litreagePair.nonEmpty, "importQty") when ask(bool, "importer", data.imports)
    packSites       <- manyT("packSites", ask(siteMapping,_), default = data.updatedProductionSites.toList)
    warehouses      <- manyT("warehouses", ask(siteMapping,_), default = data.updatedWarehouseSites.toList)
    businessAddress <- ask(addressMapping, "businessAddress", default = data.updatedBusinessAddress)
    contact         <- ask(contactDetailsMapping, "contact", data.updatedContactDetails)
  } yield data.copy (
    updatedBusinessAddress = businessAddress,
    producer               = Producer(packLarge.isDefined, packLarge),
    usesCopacker           = useCopacker.some,
    packageOwn             = packLarge.isDefined.some,
    packageOwnVol          = packQty.map{case (a,b) => Litreage(a,b)},
    copackForOthers        = copacks.isDefined,
    copackForOthersVol     = copacks.map{case (a,b) => Litreage(a,b)},
    imports                = imports.isDefined,
    importsVol             = imports.map{case (a,b) => Litreage(a,b)},
    updatedProductionSites = packSites,
    updatedWarehouseSites  = warehouses,
    updatedContactDetails  = contact
  )

  private def deregisterUpdate(
    data: VariationData
  ): WebMonad[VariationData] = for {
    reason    <- askBigText("reason")
    deregDate <- askDate("deregDate", none, constraints = List(
                           ("error.deregDate.nopast",  _ >= LocalDate.now),
                           ("error.deregDate.nofuture",_ <  LocalDate.now.plusDays(15))))

  } yield data.copy(
    reason = reason.some,
    deregDate = deregDate.some
  )

  def errorPage(id: String): WebMonad[Result] = Ok(s"Error $id")

  implicit val variationDataShow: HtmlShow[VariationData] = new HtmlShow[VariationData] {
    def showHtml(v: VariationData): Html = views.html.gdspages.variationDataCYA(v)
  }

  private def program(subscription: RetrievedSubscription, sdilRef: String)(implicit hc: HeaderCarrier): WebMonad[Result] = for {
    changeType <- askEnum("changeType", ChangeType)
    base = VariationData(subscription)
    variation  <- changeType match {
      case ChangeType.Sites      => contactUpdate(base)
      case ChangeType.Activity   => activityUpdate(base)
      case ChangeType.Deregister => deregisterUpdate(base)
    }
    _          <- when (!variation.isMaterialChange) (errorPage("noVariationNeeded"))
    _          <- tell("checkyouranswers", variation)
    _          <- execute{sdilConnector.submitVariation(Convert(variation), sdilRef)}
    exit       <- journeyEnd("variationDone")
  } yield {
    exit
  }

  def index(id: String): Action[AnyContent] = development(id)

  def real(id: String): Action[AnyContent] = registeredAction.async { implicit request =>

    val persistence = SessionCachePersistence("variation", keystore)
    val sdilRef = request.sdilEnrolment.value
    sdilConnector.retrieveSubscription(sdilRef) flatMap {
      case Some(s) =>
        runInner(request)(program(s, sdilRef))(id)(persistence.dataGet,persistence.dataPut)
      case None => NotFound("").pure[Future]
    }
  }

  val junkPersistence = new JunkPersistence

  def development(id: String): Action[AnyContent] = Action.async { implicit request =>
    val sdilRef = "XKSDIL000000000"
    val s = RetrievedSubscription("0100000000","Adam's Dyes (Also Soft Drinks)",UkAddress(List("86 Amory's Holt Way", "Ipswich"),"IP12 5ZT"),RetrievedActivity(false,true,true,true,false),LocalDate.now,List(Site(UkAddress(List("40 Sudbury Mews", "Torquay"),"TQ53 6GW"),Some("92"),None,None), Site(UkAddress(List("11B Welling Close", "North London"),"N93 9II"),Some("95"),None,None)),List(Site(UkAddress(List("33 Acre Grove", "Falkirk"),"FK38 8TP"),Some("61"),None,None)),Contact(Some("Avery Roche"),Some("Enterprise Infrastructure Engineer"),"01024 670607","Charlotte.Connolly@gmail.co.uk"))

    runInner(request)(program(s, sdilRef))(id)(junkPersistence.dataGet,junkPersistence.dataPut)
  }

  def index2(id: String): Action[AnyContent] = Action.async { implicit request =>

    val persistence = SessionCachePersistence("test", keystore)

    def testProgram: WebMonad[Result] = for {
      packSites <- manyT("packSites", askSite(_))
      exit      <- journeyEnd("variationDone")
    } yield {
      exit
    }

    runInner(request)(testProgram)(id)(persistence.dataGet,persistence.dataPut)
  }

}
