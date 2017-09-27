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
import reftree.render._
import reftree.diagram._
import reftree.contrib.SimplifiedInstances._
import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}
import scalacss.ScalatagsCss._
import scalatags.JsDom._
import scalatags.JsDom.all._
import scala.collection.immutable.{::, List, Nil, Queue, Seq}

trait HNPageModel {
  def startPage: Int
  def storiesPerPage: Int
  def topItemIDs: HNItemIDList
  def currentItems : List[(Int, HNItem)]
  def fetchRounds : List[Round]
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

  // Get the latest top stories
  def fetchTopItems() : Unit = {

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

    val fetchItems: Fetch[List[HNItem]] = pageOfItems.traverse(HNDataSources.getItem)

    val fetchResult: Future[(FetchEnv, List[HNItem])] =
      if(cache.isDefined) fetchItems.runF[Future](cache.get)
      else fetchItems.runF[Future]

    fetchResult.foreach {
      case (env, items) =>

        // Update the cache
        cache = Some(env.cache)

        // save the items as a tuple of item number and item

        val itemList = items.zipWithIndex.map {
          case (item, index) =>
            (itemNum(index), item)
        }

        // update the model with the list items and fetch rounds
        model.subProp(_.currentItems).set(itemList)
        model.subProp(_.fetchRounds).set(env.rounds.toList)
    }
  }

  // Called before view starts rendering.
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set(0)
    model.subProp(_.storiesPerPage).set(20)

      // TODO this should be called periodically and store in the model
    fetchTopItems()
  }
}


class HNPageView(model: ModelProperty[HNPageModel], presenter: HNPagePresenter) extends FinalView {
  import scalatags.JsDom.all._

  // Let's display the time like "2 minutes ago" using moment.js
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

  private val submitButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Fetch Stories")

  submitButton.listen {
    case _ =>
      presenter.fetchPageOfStories()
  }

  private val collapseButton =
    UdashButton(ButtonStyle.Default)(`type` := "button", attr("data-toggle") := "collapse", attr("data-target") := "#reftree", "Show Fetch")

  import reftree.core._

  implicit def `List RefTree`[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
    def refTree(value: List[A]): RefTree = value match {
      case head :: tail ⇒
        RefTree.Ref(value, Seq(head.refTree.toField, refTree(tail).toField)).rename("Cons")
      case Nil ⇒ RefTree.Ref(Nil, Seq.empty).rename("Nil")
    }
  }

  case class FetchInfo(count: Int, dsName: String)

  // Figure out the DataSource type and number of items fetched
  def getRoundCountAndDSName(round: Round) : List[FetchInfo] = {

    val optionList = round.request match {

      case Concurrent(queries) =>

        queries.map  {

          case FetchMany(items, dsType) =>
            Some(FetchInfo(items.toList.size, dsType.toString))
          case FetchOne(item, dsType) =>
            Some(FetchInfo(1, dsType.toString))
          case _ =>
            None

        }.toList

      case _ => List(None)
    }

    optionList.flatten

  }

  implicit def fetchInfoToRefTree: ToRefTree[FetchInfo] = ToRefTree[FetchInfo] {
    fetchInfo =>
      RefTree.Ref(fetchInfo, Seq(
        RefTree.Val(fetchInfo.count).toField.withName("count"),
        fetchInfo.dsName.refTree.toField.withName("datasource")

      ))

  }

  implicit def roundToRefTree: ToRefTree[Round] = ToRefTree[Round] {
    round =>
      val fetchInfos : List[FetchInfo] = getRoundCountAndDSName(round)

      RefTree.Ref(round, Seq(
        RefTree.Val((round.end - round.start) / 1000000).toField.withName("ms"),
        fetchInfos.refTree.toField.withName("Fetches")
      ))

  }


  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75)
  )

  import renderer._
  
  // Redraw the fetch data structure diagram
  private def renderDiagram(): Unit = {

    val lastFetch = model.subProp(_.fetchRounds).get

    Diagram.sourceCodeCaption(lastFetch).render(dom.document.getElementById("reftree"))
  }

  private val content = div(
    div(BSS.container,
      div(GlobalStyles.titleBar, BSS.row,
        span(GlobalStyles.titleBarText, "Hacker News API Fetch JS Demo")),
      div(BSS.row, GlobalStyles.controlPanel,
          UdashForm.inline(
            UdashForm.numberInput()("Start Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt)),
            UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
            submitButton.render,
            collapseButton.render
          ).render,
        div(BSS.row, GlobalStyles.reftreePanel,
          produce(model.subProp(_.fetchRounds)) { r =>
            // Redraw the fetch queue diagram
            renderDiagram
            div().render
          },
          div(id := "reftree", `class` := "collapse"))),
      div(BSS.row,
          ul(GlobalStyles.itemList,
            produce(model.subProp(_.currentItems)) {
              items => items.map {
                case (index, item) =>

                val hostName = s"${getHostName(item.url)}"

                li(GlobalStyles.itemListItem,
                  span(
                    GlobalStyles.bigGrey,
                    s"$index."
                  ),
                  a(" "),
                  a(
                    GlobalStyles.bigBlack,
                    href := item.url,
                    item.title
                  ),
                  a(" "),
                  a(
                    GlobalStyles.smallGrey,
                    href := s"https://news.ycombinator.com/from?site=$hostName",
                    hostName
                  ),
                  a(" "),
                  br,
                  div(GlobalStyles.smallGrey,
                    s"${item.score} points by",
                    a(" "),
                    a(GlobalStyles.smallGrey,
                      href := s"https://news.ycombinator.com/user?id=${item.by}",
                      item.by
                    ),
                    a(" "),
                    s"${timestampToPretty(item.time)}",
                    a(" "),
                    a(GlobalStyles.smallGrey,
                      href := s"https://news.ycombinator.com/item?id=${item.id}",
                      s"${item.descendants} comments")
                  )
                ).render
              }
            }
          )
        ).render
      )
    )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
