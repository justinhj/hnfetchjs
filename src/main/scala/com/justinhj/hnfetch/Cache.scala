package com.justinhj.hnfetch

import fetch.{DataSourceCache, DataSourceIdentity}

// An extension of DataSourceCache to include a way to get the cache size (number of elements cached)

final case class Cache(cache : Map[DataSourceIdentity, Any]) extends DataSourceCache {
  override def get[A](k: DataSourceIdentity): Option[A] =
    cache.get(k).asInstanceOf[Option[A]]

  override def update[A](k: DataSourceIdentity, v: A): Cache =
    Cache(cache.updated(k, v))

  def size : Int = cache.size
}

object Cache {
  def empty: Cache = Cache(Map.empty[DataSourceIdentity, Any])
}
