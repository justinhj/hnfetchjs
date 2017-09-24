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
import java.nio.file.Paths

import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.all.div
import io.udash.wrappers.jquery._

import scala.collection.immutable.{::, List, Nil, Queue, Seq}
import scalatags.JsDom.all._

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

        env.rounds(0).request match {

          case Concurrent(queries) =>

            queries.map {

              case FetchMany(itemz, itemType) =>
                println(s"got ${itemz.toList.size} items of type $itemType")
              case _ =>

            }

          case _ =>
        }

        //println(s"round 1 ${env.rounds(0)}")

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

  import reftree.core._

  case class Person(firstName: String, age: Int)

  implicit def treeInstance: ToRefTree[Person] = ToRefTree[Person] { person =>
    RefTree.Ref(person, Seq(
      // display the size as a hexadecimal number (why not?)
      RefTree.Val(person.age).withHint(RefTree.Val.Hex).toField.withName("Aged"),
      // highlight the value
      person.firstName.refTree.withHighlight(true).toField.withName("Nom"),
//      // do not label the children
//      tree.children.refTree.toField
    )).rename("Peep") // change the name displayed for the class
  }

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

  implicit def fetchQueue : ToRefTree[Queue[Person]] = ToRefTree[Queue[Person]] {
    case q if q.isEmpty =>
      RefTree.Null()
    case q =>
      RefTree.Ref(q, q.map(_.refTree.toField)).rename("People queue")
  }

//  implicit def fetchQueueRound : ToRefTree[Queue[Round]] = ToRefTree[Queue[Round]] {
//    case q if q.isEmpty =>
//      RefTree.Null()
//    case q =>
//      RefTree.Ref(q, q.map(_.refTree.toField)).rename("Fetch queue")
//  }

  val lastFetch = Queue(Person("Jamie", 12), Person("Lisa", 35), Person("Corbey", 45), Person("Sylvia", 49))

  //def renderDiagram() = Diagram.sourceCodeCaption(lastFetch).render(dom.document.getElementById("reftree"))
  def renderDiagram() = Diagram.sourceCodeCaption(model.subProp(_.fetchRounds).get).render(dom.document.getElementById("reftree"))

  import scalacss.ScalatagsCss._
  import scalatags.JsDom._
  import scalatags.JsDom.all._

  private val content = div(
    div(BSS.container,
      div(GlobalStyles.titleBar, BSS.row,
        span(GlobalStyles.titleBarText, "Hacker News API Fetch JS Demo")),
      div(BSS.row,
        div(BSS.Grid.colMd4,
          UdashForm(
            UdashForm.numberInput()("Start Page")(model.subProp(_.startPage).transform(_.toString, Integer.parseInt)),
            UdashForm.numberInput()("Stories per page")(model.subProp(_.storiesPerPage).transform(_.toString, Integer.parseInt)),
            submitButton.render
          ).render),
        div(BSS.Grid.colMd8,
          produce(model.subProp(_.fetchRounds)) { r =>
            // Redraw the fetch queue diagram
            renderDiagram
            div().render
          },
          div(id := "reftree"))),
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
