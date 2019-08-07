package com.selmank

case class Site(name:String, url:String, userId:String, pass:String)



object Woo {
  val STATUS_PUBLISH = "publish"
  val STATUS_DRAFT = "draft"
  val STOCK_MANAGEMENT_TRUE = true
  val STOCK_MANAGEMENT_FALSE = false
  val CONSUMER_KEY = ""
  val CONSUMER_SECRET = ""
  val HTTP_CODE_200 = "OK"
  val HTTP_CODE_201 = "CREATED"

}

object AppProperties {
  val MAX_LABEL_SIZE = 25
  val TYPE = "batch"
  val BATCH_CHUNK_SIZE = 20
  val GET_PERPAGE_NUM = 100
}

object Nonstop {
  val HOST = ""
  val KULLANICIADI = ""
  val PAS = ""
  val URL = ""
  val CONSUMER_KEY = ""
  val CONSUMER_SECRET = ""
}

object Hemi {
  val URL = ""
  val CONSUMER_KEY = ""
  val CONSUMER_SECRET = ""
}