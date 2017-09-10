package com.justinhj.views

import io.udash._
import com.justinhj._
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import org.scalajs.dom.Element
import io.udash.bootstrap.form._

import scalajs.concurrent.JSExecutionContext.Implicits.queue

trait HNPageModel {
  def startPage: String
  def storiesPerPage: String
}

case object IndexViewPresenter extends ViewPresenter[IndexState.type] {

  override def create(): (View, Presenter[IndexState.type]) = {
    val model = ModelProperty[HNPageModel]

    val presenter = new HNPagePresenter(model)
    val view = new HNPageView(model, presenter)

    (view, presenter)
  }
}

class HNPagePresenter(model: ModelProperty[HNPageModel]) extends Presenter[IndexState.type] {

  // Called before view starts rendering.
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set("0")
    model.subProp(_.storiesPerPage).set("20")
  }
}


class HNPageView(model: ModelProperty[HNPageModel], presenter: HNPagePresenter) extends FinalView {
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  val submitButton = UdashButton(ButtonStyle.Default)("Load")
  submitButton.listen {
    case _ => println(s"Fetching page ${model.subProp(_.startPage).get}")
  }

  private val content = div(
    h2("HN Fetch JS"),
    div(
      UdashForm(
        UdashForm.numberInput()("Start Page")(model.subProp(_.startPage)
        ),
        UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage)
        )).render,
      submitButton.render
    )
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
