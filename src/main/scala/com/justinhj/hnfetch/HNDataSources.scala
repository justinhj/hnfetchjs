package com.justinhj.hnfetch

import fetch.{DataSource, ExecutionType, Fetch, Query, Sequential}
import com.justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNUser, HNUserID}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

object HNDataSources {

  // Some constants to control the behaviour of Fetch executions
  // These could be moved to a config file in a real applications

  val fetchTimeout : Duration = 10 seconds // max time to wait for a single fetch
  val batchSize = Some(8) // max concurrent requests of each data source
  val executionType : ExecutionType = Sequential // whether to do batches concurrently or sequentially

  import cats.data.NonEmptyList

  implicit object HNUserSource extends DataSource[HNUserID, HNUser]{
    override def name = "user"

    override def maxBatchSize : Option[Int] = batchSize
    override def batchExecution : ExecutionType = executionType

    override def fetchOne(id: HNUserID): Query[Option[HNUser]] = {

      Query.async({
        (ok, fail) =>
          HNFetch.getUser(id) onComplete {

            case Success(futSucc) => futSucc match {
              case Right(item) =>
                println(s"GOT Item $id")
                ok(Some(item))
              case Left(err) =>
                ok(None)
            }

            case Failure(e) =>
              fail(e)
          }
      }, fetchTimeout)

    }

    // If the data source supports multiple queries (the HN API does not) you can implement it here
    // otherwise you can just tell it to use the single one using this built in function...
    override def fetchMany(ids: NonEmptyList[HNUserID]): Query[Map[HNUserID, HNUser]] = {
      batchingNotSupported(ids)
    }
  }

  implicit object HNItemSource extends DataSource[HNItemID, HNItem]{
    override def name = "item"

    override def maxBatchSize : Option[Int] = batchSize
    override def batchExecution : ExecutionType = executionType

    override def fetchOne(id: HNItemID): Query[Option[HNItem]] = {
      Query.async({
        (ok, fail) =>
          println(s"GET Item $id")
          HNFetch.getItem(id) onComplete {

            case Success(futSucc) => futSucc match {
              case Right(item) =>
                println(s"GOT Item $id")
                ok(Some(item))
              case Left(err) =>
                ok(None)
            }

            case Failure(e) =>
              fail(e)
          }
      }, fetchTimeout)
    }

    // If the data source supports multiple queries (the HN API does not) you can implement it here
    // otherwise you can just tell it to use the single one using this built in function...
    override def fetchMany(ids: NonEmptyList[HNItemID]): Query[Map[HNItemID, HNItem]] = {
      batchingNotSupported(ids)
    }
  }

  def getUser(id: HNUserID): Fetch[HNUser] = Fetch(id)
  def getItem(id: HNItemID): Fetch[HNItem] = Fetch(id)

}
