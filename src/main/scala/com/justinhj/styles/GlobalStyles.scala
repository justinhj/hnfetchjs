package com.justinhj.styles

import scala.language.postfixOps
import scalacss.internal.{Attr, Literal}
import scalacss.DevDefaults._
import scalacss.internal.Compose

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  object Colors {
    val hnGreyB = c"#f6f6ef"
    val hnOrangeB = c"#ff6600"
  }

  val titleBar = style(
    backgroundColor(Colors.hnOrangeB),
    marginTop(10 px)
  )

  val itemList = style(
    backgroundColor(Colors.hnGreyB),
    marginTop(20 px)
  )


}
