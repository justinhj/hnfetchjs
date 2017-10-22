package com.justinhj.views

import cats.instances.list._
import cats.syntax.traverse._
import com.justinhj._
import com.justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNItemIDList}
import com.justinhj.hnfetch.{Cache, HNDataSources, HNFetch, HNRefTree}
import com.justinhj.styles._
import fetch.implicits._
import fetch.syntax._
import fetch.{DataSourceCache, FetchEnv, _}
import io.udash._
import io.udash.bootstrap.button.{ButtonStyle, UdashButton}
import io.udash.bootstrap.form._
import io.udash.bootstrap.{BootstrapStyles => BSS}
import io.udash.css.CssStyleName

import scala.collection.immutable.List
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import scalacss.ScalatagsCss._

// Define the data model for our page
trait HNPageModel {
  def startPage: Int // First page of stories to fetch and display (starts at 1)
  def storiesPerPage: Int // Number of stories to fetch and dispaly per page
  def topItemIDs: HNItemIDList // List of item IDs of top stories
  def currentItems : List[(Int, HNItem)] // Fetched items and current rank
  def fetchRounds : List[Round] // The Rounds from the last Fetch
  def cacheSize : Int // size of the cache
  def storyCount: Int // number of top stories
}

// ViewPresenter - responsible for creating the Presenter/View and Model
case object IndexViewPresenter extends ViewPresenter[IndexState.type] {

  override def create(): (View, Presenter[IndexState.type]) = {
    val model = ModelProperty[HNPageModel]

    val presenter = new HNPagePresenter(model)
    val view = new HNPageView(model, presenter)

    (view, presenter)
  }
}

// Presenter - responsible for the business logic, in this case managing fetching of data
// via the Hacker News API
class HNPagePresenter(model: ModelProperty[HNPageModel]) extends Presenter[IndexState.type] {

  var cache : DataSourceCache = Cache.empty

  def flushCache() : Unit = {
    cache = Cache.empty
    model.subProp(_.cacheSize).set(0)
  }

  // Get the latest top stories IDs
  def getTopItemsIDs() : Unit = {

    HNFetch.getTopItems().map {
      case Right(good) =>
        model.subProp(_.topItemIDs).set(good)
        model.subProp(_.storyCount).set(good.size)

      case Left(bad) =>
        // TODO Could display error dialog but for now this just logs to console
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

    // Get the items we will fetch for this particular page
    val pageOfItems = hnItemIDlist.slice((startPage-1) * numItemsPerPage, (startPage-1) * numItemsPerPage + numItemsPerPage)

    // Turn them into Fetch jobs
    val fetchItems: Fetch[List[HNItem]] = pageOfItems.traverse(HNDataSources.getItem)

    // Run the fetch
    val fetchResult: Future[(FetchEnv, List[HNItem])] = fetchItems.runF[Future](cache)

    // Update our model based on the result
    // The changes to the model will be automatically updated on the page thanks to Udash
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

  // Called before view starts rendering, initialize things
  override def handleState(state: IndexState.type): Unit = {

    model.subProp(_.startPage).set(1)
    model.subProp(_.storiesPerPage).set(30)
    model.subProp(_.cacheSize).set(0)
    model.subProp(_.topItemIDs).set(List())
    model.subProp(_.currentItems).set(List())
    model.subProp(_.fetchRounds).set(List())
    model.subProp(_.storyCount).set(0)

    getTopItemsIDs()
  }
}

// FinalView - how to render the page
class HNPageView(model: ModelProperty[HNPageModel], presenter: HNPagePresenter) extends FinalView {
  import scalatags.JsDom.all._

  private val fetchStoriesButton: UdashButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Fetch Page")

  fetchStoriesButton.listen {
    case _ =>
      presenter.fetchPageOfStories()
  }

  private val refreshTopStoriesButton: UdashButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Refresh Top Stories")

  refreshTopStoriesButton.listen {
    case _ =>
      presenter.getTopItemsIDs()
  }

  private val flushCacheButton: UdashButton = UdashButton(ButtonStyle.Default)(`type` := "button", "Flush cache")

  flushCacheButton.listen {
    case _ =>
      presenter.flushCache()
  }

  val test = 9223372036854775807L

  val tabContent = CssStyleName("tab-content")
  val tabPane = CssStyleName("tab-pane")

  import io.udash.css.CSSView.

  private val content = div(
    div(BSS.container,
      div(InlineStyles.titleBar, BSS.row,
        a(href := "https://github.com/justinhj/hnfetchjs",
          img(src := "assets/images/GitHub-Mark-32px.png")),
        a(InlineStyles.titleBarText, href := "https://github.com/justinhj/hnfetchjs", "clone hnfetchjs on github"),
        span(InlineStyles.titleBarTextSmall, "Cached items : ", bind(model.subProp(_.cacheSize))),
        span(InlineStyles.titleBarTextSmall, " Number of top stories : "),
        span(InlineStyles.titleBarTextSmall, bind(model.subProp(_.storyCount)))),
      div(BSS.row, InlineStyles.background,
          UdashForm.inline(
            UdashForm.numberInput()(InlineStyles.label, "Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt), InlineStyles.input),
            UdashForm.numberInput()(InlineStyles.label, "Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
            fetchStoriesButton.render,
            refreshTopStoriesButton.render,
            flushCacheButton.render
          ).render),
      ul(BSS.row, BSS.Navigation.nav, BSS.Navigation.navTabs, InlineStyles.background, id := "my-tabs",
        li(role := "presentation", BSS.active,
          a(href := "#itemlist", attr("data-toggle") := "tab", "Stories")),
        li(
          a(role := "presentation", href := "#reftree", attr("data-toggle") := "tab", "Last Fetch"))
      ),
      div(BSS.row, tabContent, BSS.Visibility.clearfix,

        div(tabPane, id := "reftree", role := "tabpanel", InlineStyles.reftreePanel,
          produce(model.subProp(_.fetchRounds)) { r =>
            // Redraw the fetch queue diagram
            HNRefTree.renderDiagram("reftree", r)
            div().render
          }),

        div(tabPane, BSS.active, role := "tabpanel", id := "itemlist",
            ul(InlineStyles.itemList,
              produce(model.subProp(_.currentItems)) {
                items => items.map {
                  case (index, item) =>

                  val hostName = s"${Util.getHostName(item.url)}"

                  li(InlineStyles.itemListItem,
                    span(
                      InlineStyles.bigGrey,
                      s"$index."
                    ),
                    a(" "),
                    a(
                      InlineStyles.bigBlack,
                      href := item.url,
                      item.title
                    ),
                    a(" "),
                    a(
                      InlineStyles.smallGrey,
                      href := s"https://news.ycombinator.com/from?site=$hostName",
                      hostName
                    ),
                    a(" "),
                    br,
                    div(InlineStyles.smallGrey,
                      s"${item.score} points by",
                      a(" "),
                      a(InlineStyles.smallGrey,
                        href := s"https://news.ycombinator.com/user?id=${item.by}",
                        item.by
                      ),
                      a(" "),
                      s"${Util.timestampToPretty(item.time)}",
                      a(" "),
                      a(InlineStyles.smallGrey,
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
  )

  override def getTemplate: Modifier = content

  override def renderChild(view: View): Unit = {}
}
