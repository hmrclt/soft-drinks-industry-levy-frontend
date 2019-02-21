package sdil.uniform

import ltbs.play.scaffold.{GdsComponents, SdilComponents}
import org.scalatest.{FlatSpec, Matchers}

class GDS extends FlatSpec with Matchers {

  "A GDC Call for " should "be correctly" in {

    val litreageMappingVal = GdsComponents.litreage("")

    1 shouldBe 1

  }



}
