package com.justinhj.views

import io.udash._
import com.justinhj._
import com.justinhj.hnfetch.HNFetch
import com.justinhj.hnfetch.HNFetch.HNItemIDList
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import org.scalajs.dom.Element
import io.udash.bootstrap.form._
import org.scalajs.dom.ext.Ajax

import scala.util.{Failure, Success, Try}
import scalajs.concurrent.JSExecutionContext.Implicits.queue

trait HNPageModel {
  def startPage: String
  def storiesPerPage: String
  def topItemIDs: HNItemIDList
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

  // Called on initialization and ??? to get the latest top stories
  def fetchTopItems() : Unit = {

    // Todo this should fetch a page of stories instead
    HNFetch.getTopItems().map {
      res => res match {

        case Right(good) =>
          model.subProp(_.topItemIDs).set(good)
          //println(good)
        case Left(bad) =>
          // TODO display error dialog
          println(s"Error: $bad")
      }
    }

  }

  def fetchPageOfStories(): Unit = {

    // Todo this should fetch a page of stories instead

    val start = Try(model.subProp(_.startPage).get.toInt).getOrElse(0)
    val perPage = Try(model.subProp(_.storiesPerPage).get.toInt).getOrElse(10)

    println(s"Trigger fetch of $perPage items starting at page $start")

  }

  // Called before view starts rendering.
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set("0")
    model.subProp(_.storiesPerPage).set("20")
    //model.subProp(_.topItemIDs).set("")

    fetchTopItems()
  }
}


class HNPageView(model: ModelProperty[HNPageModel], presenter: HNPagePresenter) extends FinalView {
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  val submitButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Load")

  submitButton.listen {
    case _ =>
      presenter.fetchPageOfStories()

  }

  private val content = div(
    h3("Hacker News API with Fetch"),
    div(cls := "col-md-6",
      UdashForm(
        UdashForm.numberInput()("Start Page")(model.subProp(_.startPage)),
        UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage)),
        submitButton.render
      ).render
      //,
//      div(cls := "col-md-6",
//        pre(model.subProp(_.output).get)
//        )
    )
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
