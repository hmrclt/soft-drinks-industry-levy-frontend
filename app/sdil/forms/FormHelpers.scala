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

package sdil.forms

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import sdil.models.Address

trait FormHelpers {
  private lazy val postcodeRegex = """^([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z]))))\s?[0-9][A-Za-z]{2})$"""

  lazy val postcode: Mapping[String] = text.verifying(Constraint { x: String => x match {
    case "" => Invalid("error.postcode.required")
    case pc if !pc.matches(postcodeRegex) => Invalid("error.postcode.invalid")
    case _ => Valid
  }})

  lazy val addressMapping: Mapping[Address] = mapping(
    "line1" -> mandatoryAddressLine("line1"),
    "line2" -> mandatoryAddressLine("line2"),
    "line3" -> optionalAddressLine,
    "line4" -> optionalAddressLine,
    "postcode" -> postcode
  )(Address.apply)(Address.unapply)

  private def mandatoryAddressLine(key: String): Mapping[String] = {
    text.verifying(combine(required(key), optionalAddressLineConstraint))
  }

  private lazy val optionalAddressLine: Mapping[String] = {
    text.verifying(optionalAddressLineConstraint)
  }

  private lazy val optionalAddressLineConstraint: Constraint[String] = Constraint {
    case a if a.length > 35 => Invalid("error.address-line.length")
    case a if !a.matches("""^[A-Za-z0-9 \-,.&'\/]*$""") => Invalid("error.address-line.invalid")
    case _ => Valid
  }

  def required(key: String): Constraint[String] = Constraint {
    case "" => Invalid(s"error.$key.required")
    case _ => Valid
  }

  lazy val mandatoryBoolean: Mapping[Boolean] = optional(boolean)
    .verifying("error.radio-form.choose-option", _.nonEmpty).transform(_.get, Some.apply)

  def combine[T](c1: Constraint[T], c2: Constraint[T]): Constraint[T] = Constraint { v =>
    c1.apply(v) match {
      case Valid => c2.apply(v)
      case i: Invalid => i
    }
  }
}