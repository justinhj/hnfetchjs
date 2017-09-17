package com.justinhj.views

import io.udash._
import com.justinhj.RootState
import org.scalajs.dom
import org.scalajs.dom.Element

import scalatags.JsDom.tags2.main

object RootViewPresenter extends DefaultViewPresenterFactory[RootState.type](() => new RootView)

class RootView extends View {
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  private val child: Element = div().render

  private def loadBootstrapStyles(): dom.Element =
    link(rel := "stylesheet", href := "assets/bootstrap/css/bootstrap.min.css").render

  private def loadExternalJS(): dom.Element =
    script(src := "assets/js/moment.js").render

  private val content = div(
    loadBootstrapStyles(),
    main(
      child
    ),
    loadExternalJS()
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {
    import io.udash.wrappers.jquery._
    jQ(child).children().remove()
    view.getTemplate.applyTo(child)
  }
}