package com.justinhj.styles

import scala.language.postfixOps
import scalacss.internal.{Attr, Literal}
import scalacss.DevDefaults._
import scalacss.internal.Compose

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  object Colors {
    val hnGrey = c"#f6f6ef"
    val hnGreyDark = c"#828282"
    val hnOrange = c"#ff6600"
  }

  val bigScale = 1.1

  import Colors._

  val titleBar = style(
    backgroundColor(hnOrange),
    marginBottom(20 px)
  )

  val bigGrey = style(
    fontSize(bigScale em),
    color(hnGreyDark)
  )

  val bigBlack = style(
    fontSize(bigScale em),
    color.black
  )

  val smallGrey = style(
    color(hnGreyDark)
  )

  val itemList = style(
    backgroundColor(hnGrey),
    marginTop(20 px)
  )

  val itemListItem = style(
    marginTop(8 px)
  )


}
