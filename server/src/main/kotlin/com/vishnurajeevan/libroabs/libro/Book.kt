package com.vishnurajeevan.libroabs.libro

import kotlinx.serialization.Serializable

@Serializable
data class Book(
  val authors: List<String>,
  val isbn: String,
  val series: String?,
  val series_num: Int?,
  val title: String
)