package com.justinhj.styles

import scala.language.postfixOps
import scalacss.internal.{Attr, Compose, FontFace, Literal}
import scalacss.DevDefaults._

object StandaloneStyles extends StyleSheet.Standalone {

  import dsl._
  import InlineStyles.Colors._

  "#my-tabs>li.active>a" - (
    color(hnGreyDarker)
    )

  "#my-tabs>li>a" - (
      color(hnGreyDark)
  )

}

object InlineStyles extends StyleSheet.Inline {
  import dsl._

  object Colors {
    val hnGrey = c"#f6f6ef"
    val hnGreyDark = c"#828282"
    val hnGreyDarker = c"#424242"
    val hnOrange = c"#ff6600"
  }

  object Font {
    val fonts = "Verdana, Geneva, sans-serif;"
  }

  val bigScale = 1.0
  val smallScale = 0.8

  import Colors._
  import Font._

  val input = style {
    width(30 px)
  }

  val background = style {
    backgroundColor(hnGrey)
  }

  val reftreePanel = style {
    backgroundColor.white
  }

  val titleBar = style(
    backgroundColor(hnOrange),
    //marginBottom(5 px),
    paddingTop(5 px),
    paddingBottom(5 px),
    paddingLeft(5 px)
  )

  val titleBarText = style(
    fontSize(bigScale em),
    color.black,
    fontWeight.bold,
    fontFamily :=! fonts
  )

  val titleBarTextSmall = style(
    fontSize(smallScale em),
    color(hnGreyDarker),
    fontWeight.normal,
    fontFamily :=! fonts
  )

  val bigGrey = style(
    fontSize(bigScale em),
    color(hnGreyDark),
    fontFamily :=! fonts
  )

  val bigBlack = style(
    fontSize(bigScale em),
    color.black,
    fontFamily :=! fonts
  )

  val smallGrey = style(
    color(hnGreyDark),
    fontFamily :=! fonts,
    fontSize(smallScale em)
  )

  val itemList = style(
    backgroundColor(hnGrey)
    //marginTop(5 px)
  )

  val itemListItem = style(
      //marginTop(8 px)
  )


}
