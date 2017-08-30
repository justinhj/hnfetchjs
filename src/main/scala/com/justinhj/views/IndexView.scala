package com.justinhj.views

import io.udash._
import com.justinhj._
import org.scalajs.dom.Element

object IndexViewPresenter extends DefaultViewPresenterFactory[IndexState.type](() => new IndexView)

class IndexView extends View {
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  private val content = div(
    h2("HN Fetch JS")    
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
