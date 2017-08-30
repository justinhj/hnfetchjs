package com.justinhj.views

import io.udash._
import com.justinhj._
import org.scalajs.dom.Element

object IndexViewPresenter extends DefaultViewPresenterFactory[IndexState.type](() => new IndexView)

class IndexView extends View {
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  private val content = div(
    h2("Thank you for choosing Udash! Take a look at following demo pages:"),
    ul(
      
    ),
    h3("Read more"),
    ul(
      li(
        a(href := "http://udash.io/", target := "_blank")("Visit Udash Homepage.")
      ),
      li(
        a(href := "http://guide.udash.io/", target := "_blank")("Read more in Udash Guide.")
      ),
      li(
        a(href := "https://www.scala-js.org/", target := "_blank")("Read more about Scala.js.")
      ),
      li(
        a(href := "https://japgolly.github.io/scalacss/book/", target := "_blank")("Read more about ScalaCSS")
      ),
      li(
        a(href := "http://www.lihaoyi.com/scalatags/", target := "_blank")("Read more about ScalaTags")
      )
    )
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}