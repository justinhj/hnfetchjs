package com.justinhj.views

import java.net.URI

import cats.instances.list._
import cats.syntax.traverse._
import com.justinhj._
import com.justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNItemIDList}
import com.justinhj.hnfetch.{HNDataSources, HNFetch}
import com.justinhj.styles._
import fetch.implicits._
import fetch.syntax._
import fetch.{DataSourceCache, FetchEnv, InMemoryCache, _}
import io.udash._
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import io.udash.bootstrap.form._
import io.udash.bootstrap.label.UdashLabel
import io.udash.bootstrap.{UdashBootstrap, BootstrapStyles => BSS}
import moment._
import org.scalajs.dom
import reftree.contrib.SimplifiedInstances._
import reftree.diagram._
import reftree.render._

import scala.collection.immutable.{List, Seq}
import scala.collection.mutable
import scala.collection.parallel.immutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}
import scalacss.ScalatagsCss._

trait HNPageModel {
  def startPage: Int
  def storiesPerPage: Int
  def topItemIDs: HNItemIDList
  def currentItems : List[(Int, HNItem)]
  def fetchRounds : List[Round]
  def cacheSize : Int
  def storyCount: Int
}

case object IndexViewPresenter extends ViewPresenter[IndexState.type] {

  override def create(): (View, Presenter[IndexState.type]) = {
    val model = ModelProperty[HNPageModel]

    val presenter = new HNPagePresenter(model)
    val view = new HNPageView(model, presenter)

    (view, presenter)
  }
}

// TEMP this can go to its own file CacheUtil

final case class Cache(cache : Map[DataSourceIdentity, Any]) extends DataSourceCache {
  override def get[A](k: DataSourceIdentity): Option[A] =
    cache.get(k).asInstanceOf[Option[A]]

  override def update[A](k: DataSourceIdentity, v: A): Cache =
    Cache(cache.updated(k, v))

  def size() = cache.size
}

object Cache {

  def empty: Cache = Cache(Map.empty[DataSourceIdentity, Any])

}

class HNPagePresenter(model: ModelProperty[HNPageModel]) extends Presenter[IndexState.type] {

  var cache : DataSourceCache = Cache.empty

  // Get the latest top stories id's
  def fetchTopItems() : Unit = {

    HNFetch.getTopItems().map {
      case Right(good) =>
        model.subProp(_.topItemIDs).set(good)
        model.subProp(_.storyCount).set(good.size)

      case Left(bad) =>
        // TODO Could display error dialog but this just logs to console
        println(s"Error: $bad")
    }
  }

  // fetch a page of stories based on current page number and stories per page
  def fetchPageOfStories(): Unit = {

    val startPage = Try(model.subProp(_.startPage).get).getOrElse(1)
    val numItemsPerPage = Try(model.subProp(_.storiesPerPage).get).getOrElse(10)
    val hnItemIDlist = Try(model.subProp(_.topItemIDs).get).getOrElse(List.empty[HNItemID])

    // helper to show the article rank
    def itemNum(n: Int) = ((startPage -1) * numItemsPerPage) + n + 1

    // Get the items
    val pageOfItems = hnItemIDlist.slice((startPage-1) * numItemsPerPage, (startPage-1) * numItemsPerPage + numItemsPerPage)

    val fetchItems: Fetch[List[HNItem]] = pageOfItems.traverse(HNDataSources.getItem)

    val fetchResult: Future[(FetchEnv, List[HNItem])] = fetchItems.runF[Future](cache)

    fetchResult.foreach {
      case (env, items) =>

        // Update the cache
        cache = env.cache

        // save the items as a tuple of item number and item

        val itemList = items.zipWithIndex.map {
          case (item, index) =>
            (itemNum(index), item)
        }

        // update the model with the list items and fetch rounds
        model.subProp(_.currentItems).set(itemList)
        model.subProp(_.fetchRounds).set(env.rounds.toList)
        model.subProp(_.cacheSize).set(cache.asInstanceOf[Cache].size)
    }
  }

  // Called before view starts rendering.
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set(1)
    model.subProp(_.storiesPerPage).set(30)
    model.subProp(_.cacheSize).set(0)
    model.subProp(_.topItemIDs).set(List())
    model.subProp(_.currentItems).set(List())
    model.subProp(_.fetchRounds).set(List())
    model.subProp(_.storyCount).set(0)

      // TODO this also should be called periodically and store in the model
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

  private val submitButton: UdashButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Fetch Stories")

  submitButton.listen {
    case _ =>
      presenter.fetchPageOfStories()
  }

  private val collapseButton =
    UdashButton(ButtonStyle.Default)(`type` := "button", attr("data-toggle") := "collapse", attr("data-target") := "#reftree", "Show Fetch")

  import reftree.core._

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


//  def combinedProp = model.subProp(_.topItemIDs).combine(model.subProp(_.storiesPerPage)){
//    (topItems, storiesPerPage) =>
//      s"hello ${topItems.size} + ${storiesPerPage}"
//  }

//  private def topStoriesCount : ReadableProperty[String] =
//    model.subProp(_.topItemIDs).transform((topStories: List[HNFetch.HNItemID]) => topStories.size.toString)

  private val content = div(
    div(BSS.container,
      div(GlobalStyles.titleBar, BSS.row,
        span(GlobalStyles.titleBarText, "Hacker News API Fetch JS Demo "),
        span(GlobalStyles.smallGrey, "Cached items : ", bind(model.subProp(_.cacheSize)))
      ),
      div(BSS.row, GlobalStyles.controlPanel,
          UdashForm.inline(
            UdashForm.numberInput()("Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt), GlobalStyles.input),
            UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
            submitButton.render,
            collapseButton.render
          ).render,
        div(BSS.row,
          "Stories in cache ",
          b(bind(model.subProp(_.cacheSize))),
          " number of top stories ",
          b(bind(model.subProp(_.storyCount)))
          )
          .render,
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
