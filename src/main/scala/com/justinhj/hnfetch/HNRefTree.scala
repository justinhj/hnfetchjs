package com.justinhj.hnfetch

import fetch.{Concurrent, FetchMany, FetchOne, Round}
import org.scalajs.dom
import reftree.diagram.Diagram
import reftree.render.{Renderer, RenderingOptions}
import reftree.core._
import reftree.contrib.SimplifiedInstances.{string, list}

import scala.collection.immutable.{List, Seq}

// Utilities for drawing a reftree of Fetch rounds

object HNRefTree {

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

  private val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75)
  )

  import renderer._

  // Redraw the fetch data structure diagram
  def renderDiagram(elementID : String, rounds: List[Round]): Unit = {
    
    Diagram.sourceCodeCaption(rounds).render(dom.document.getElementById(elementID))
  }

  case class FetchInfo(count: Int, dsName: String)

  // Figure out the DataSource type and number of items fetched
  def getRoundCountAndDSName(round: Round) : List[FetchInfo] = {

    val optionList = round.request match {

      case Concurrent(queries) =>

        queries.map  {

          case FetchMany(items, dsType) =>
            Some(FetchInfo(items.toList.size, dsType.toString))
          case FetchOne(_, dsType) =>
            Some(FetchInfo(1, dsType.toString))
          case _ =>
            None

        }.toList

      case _ => List(None)
    }

    optionList.flatten

  }

}
