package com.justinhj.views

import java.net.URI
import java.util.Date

import io.udash._
import com.justinhj._
import com.justinhj.hnfetch.{HNDataSources, HNFetch}
import com.justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNItemIDList}
import fetch.{DataSourceCache, FetchEnv}
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import org.scalajs.dom.Element
import io.udash.bootstrap.form._
import org.scalajs.dom.ext.Ajax

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalatags.JsDom.all.li
import cats.instances.list._
import fetch._
import fetch.implicits._
import cats.instances.list._
import cats.syntax.traverse._
import fetch.syntax._

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

    val startPage = Try(model.subProp(_.startPage).get.toInt).getOrElse(0)
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

//        items.zipWithIndex.foreach {
//          case (item, n) =>
//            println(s"${itemNum(n)}. ${item.title} ${getHostName(item.url)}")
//            println(s"  ${item.score} points by ${item.by} at ${timestampToPretty(item.time)} ${item.descendants} comments\n")
//        }

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
  import com.justinhj.Context._
  import scalatags.JsDom.all._

  // Let's display the time like "2 minutes ago" using the PrettyTime library
  // ts is epoch time in seconds
  def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    "Some time ago"

    //    val p = new PrettyTime()
    //    p.format(new Date(epochTimeMS))
  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URI(url)) match {
        case Success(u) =>
          "(" + u.getHost + ")"
        case Failure(e) =>
          ""
      }
    }
  }

  val submitButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Load")

  submitButton.listen {
    case _ =>
      presenter.fetchPageOfStories()

  }

  private val content = div(
    h3("Hacker News API with Fetch"),
    div(cls := "col-md-3",
      UdashForm(
        UdashForm.numberInput()("Start Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt)),
        UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
        submitButton.render
      ).render,
      div(cls := "col-md-10",
        ul(
          produce(model.subProp(_.currentItems)) {
            items => items.map {
              case item =>
                val line1 = s"xx. ${item.title} ${getHostName(item.url)}"
                val line2 = s"  ${item.score} points by ${item.by} at ${timestampToPretty(item.time)} ${item.descendants} comments\n"

                li(line1, br, line2).render
            }
          }
        )
      )
    )
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
