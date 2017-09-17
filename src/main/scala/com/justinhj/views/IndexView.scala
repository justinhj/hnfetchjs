package com.justinhj.views

import java.net.URI

import com.justinhj.styles._
import cats.instances.list._
import cats.syntax.traverse._
import com.justinhj._
import com.justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNItemIDList}
import com.justinhj.hnfetch.{HNDataSources, HNFetch}
import io.udash.bootstrap.{BootstrapStyles => BSS}
import fetch.implicits._
import fetch.syntax._
import fetch.{DataSourceCache, FetchEnv, _}
import io.udash._
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import io.udash.bootstrap.form._
import moment._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

trait HNPageModel {
  def startPage: Int
  def storiesPerPage: Int
  def topItemIDs: HNItemIDList
  def currentItems : List[HNItem]
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

  var cache: Option[DataSourceCache] = None

  // Called on initialization and ??? to get the latest top stories
  def fetchTopItems() : Unit = {

    // Todo this should fetch a page of stories instead
    HNFetch.getTopItems().map {
      case Right(good) =>
        model.subProp(_.topItemIDs).set(good)
      case Left(bad) =>
        // TODO display error dialog
        println(s"Error: $bad")
    }

  }

  def fetchPageOfStories(): Unit = {

    val startPage = Try(model.subProp(_.startPage).get).getOrElse(0)
    val numItemsPerPage = Try(model.subProp(_.storiesPerPage).get).getOrElse(10)
    val hnItemIDlist = Try(model.subProp(_.topItemIDs).get).getOrElse(List.empty[HNItemID])

    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    // Get the items
    val pageOfItems = hnItemIDlist.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

    val fetchResult: Future[(FetchEnv, List[HNItem])] =
      if(cache.isDefined) fetchItems.runF[Future](cache.get)
      else fetchItems.runF[Future]

    fetchResult.foreach {
      case (env, items) =>

        // Update the cache
        cache = Some(env.cache)
        // env.rounds (may need me later)

        // update the model
        model.subProp(_.currentItems).set(items)
    }
  }

  // Called before view starts rendering.
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set(0)
    model.subProp(_.storiesPerPage).set(20)
    //model.subProp(_.topItemIDs).set("")

    fetchTopItems()
  }
}


class HNPageView(model: ModelProperty[HNPageModel], presenter: HNPagePresenter) extends FinalView {
  import scalatags.JsDom.all._

  // Let's display the time like "2 minutes ago" using the PrettyTime library
  // ts is epoch time in seconds
  def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    Moment(epochTimeMS).fromNow()

  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URI(url)) match {
        case Success(uri) =>
          "(" + uri.getHost + ")"
        case Failure(_) =>
          "[parse error]"
      }
    }
  }

  private val submitButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Load")

  submitButton.listen {
    case _ =>
      presenter.fetchPageOfStories()

  }

  import scalacss.DevDefaults._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom._
  import scalatags.JsDom.all._

  private val content = div(
    div(BSS.container,
      h3("Hacker News Fetch"),
      div(BSS.row,
        UdashForm(
          UdashForm.numberInput()("Start Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt)),
          UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
          submitButton.render
        ).render),
      div(BSS.row,
          ul(
            produce(model.subProp(_.currentItems)) {
              items => items.map {
                item =>
                val line1 = s"xx. ${item.title} ${getHostName(item.url)}"
                val line2 = s"  ${item.score} points by ${item.by} ${timestampToPretty(item.time)} ${item.descendants} comments\n"

                li(GlobalStyles.listBackground, line1, br, line2).render
              }
            }
          )
        ).render
      )
    )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
