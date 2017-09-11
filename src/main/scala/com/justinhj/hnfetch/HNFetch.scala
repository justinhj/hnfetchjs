package com.justinhj.hnfetch

import java.lang.Throwable

import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalajs.concurrent.JSExecutionContext.Implicits.queue


// Get Hacker News items

object HNFetch {

  val baseHNURL = "https://hacker-news.firebaseio.com/v0/"

  type HNUserID = String
  type HNItemID = Int

  val HNMissingItemID : HNItemID = -1
  val HNMissingUserID : HNUserID = ""

  case class HNUser (
                      id : HNUserID, // The user's unique username. Case-sensitive. Required.
                      //delay : Int, // Delay in minutes between a comment's creation and its visibility to other users.
                      created : Int, // Creation date of the user, in Unix Time.
                      karma : Int, // The user's karma.
                      about : String, // The user's optional self-description. HTML.
                      submitted : List[HNItemID] ) // List of the user's stories, polls and comments.

  // constuct url for api queries
  def getUserURL(userId: HNUserID) = s"${baseHNURL}user/$userId.json"
  def getItemURL(itemId: HNItemID) = s"${baseHNURL}item/$itemId.json"
  val getTopItemsURL = s"${baseHNURL}topstories.json"

  case class HNItem(
                     id : HNItemID, // The item's unique id.
                     deleted : Boolean = false, // true if the item is deleted.
                     `type` : String, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
                     by : HNUserID = HNMissingUserID, // The username of the item's author.
                     time : Int, // Creation date of the item, in Unix Time.
                     text : String = "", // The comment, story or poll text. HTML.
                     dead : Boolean = false, // true if the item is dead.
                     parent : HNItemID = HNMissingItemID, // The comment's parent: either another comment or the relevant story.
                     poll : HNItemID = HNMissingItemID, // The pollopt's associated poll.
                     kids : List[HNItemID] = List(), // The ids of the item's comments, in ranked display order.
                     url : String = "", // The URL of the story.
                     score : Int = -1, // The story's score, or the votes for a pollopt.
                     title : String = "", // The title of the story, poll or job.
                     parts : List[HNItemID] = List(), // A list of related pollopts, in display order.
                     descendants : Int = 0 // In the case of stories or polls, the total comment count.
                   )

  // constuct the query to get an item
  def getUser(userID: HNUserID) : Future[Either[String, HNUser]] = {
    val url = getUserURL(userID)

    //println(s"GET $url")
    hnRequest[HNUser](url)
  }

  def getItem(itemId: HNItemID) : Future[Either[String, HNItem]] = {
    val url = getItemURL(itemId)

    //println(s"GET $url")
    hnRequest[HNItem](url)
  }

  type HNItemIDList = List[HNItemID]

  def getTopItems() : Future[Either[String, HNItemIDList]] = {
    hnRequest[HNItemIDList](getTopItemsURL)
  }

  def hnRequest[T](url: String)(implicit r: Reader[T]) : Future[Either[String, T]] = {

    Ajax.get(url).map {
      xhr =>
        // Parse the response
        Try(read[T](xhr.responseText)) match {
          case Success(good) if good == null =>
            Left("Not found")
          case Success(good) =>
            Right(good)
          case Failure(err) =>
            Left(err.getMessage())
        }
    }.recover {
      case err : Throwable =>
        Left(err.getMessage())
    }

  }

}
