package foo

import org.scalatra._

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date

import io.circe.{Json, Printer, parser}
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.jawn.decode
import scalaj.http.{Http, HttpOptions, HttpResponse, Token}
import com.selmank._
import comp._
import herokuComp._

import better.files._
import File._
import java.io.{FileInputStream, IOException, InputStream, File => JFile}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.duration._


class foo extends ScalatraServlet with AuthenticationSupport {

  get("/") {
    views.html.hello()
  }

  get("/json") {
    basicAuth()
    val products = getAllProducts(foo.nonstop)
    println(products.length)
    val file = File("hello.txt")
    file.createIfNotExists()
    val str = products.asJson.toString()
    file.overwrite(str)//(charset = UnicodeCharset("UTF-8", writeByteOrderMarkers = true))
    val gzipped = file.gzipTo(File("hello.txt.gz"))
    gzipped
  }

  get("/json2") {
    basicAuth()
    val products = getAllProducts(foo.nonstop)
    val str = products.asJson.toString()
    str
  }

  //Upload to server
  get("/nonstop") {
    basicAuth()
    val site = foo.nonstop
    val fileName = site.name
    val products = getAllProducts(site)
    val file = File(s"${fileName}.json")
    file.createIfNotExists()
    val str = products.asJson.toString()
    file.overwrite(str)
    val gzipped = file.gzipTo(File(s"${fileName}.json.gz"))
    uploadFileWithBackup(gzipped)(site)
  }

  //Upload to server
  get("/hemi") {
    basicAuth()
    val site = foo.hemi
    val fileName = site.name
    val allProducts = getAllProducts(site)
    val publishedProducts = getPublishedProducts(allProducts)

    val publishedStr = publishedProducts.asJson.toString()
    val md5OfPublished = Utils.md5HashString(publishedStr)


    val file2 = File(s"${fileName}_published.json")
    file2.createIfNotExists()
    file2.overwrite(publishedStr)
    val gzipped2 = file2.gzipTo(File(s"${fileName}_published.json.gz"))
    uploadFileWithBackup(gzipped2)(site,".com/ios-app")

    val md5FileOfPublished = FileUtils.createFile(md5OfPublished)(s"${fileName}_published.md5")
    uploadFileWithBackup(md5FileOfPublished)(site,"")
  }

  get("/hemi-all") {
    basicAuth()
    val site = foo.hemi
    val fileName = site.name
    val allProducts = getAllProducts(site)

    val allStr = allProducts.asJson.toString()
    val md5OfAll = Utils.md5HashString(allStr)

    val file = File(s"${fileName}_all.json")
    file.createIfNotExists()
    file.overwrite(allStr)

    val gzipped = file.gzipTo(File(s"${fileName}_all.json.gz"))
    uploadFileWithBackup(gzipped)(site,"")

    val md5FileOfAll = FileUtils.createFile(md5OfAll)(s"${fileName}_all.md5")
    uploadFileWithBackup(md5FileOfAll)(site,"")
  }
}

object foo {
  val hemi:Site = Site("hemi",Hemi.URL,Hemi.CONSUMER_KEY,Hemi.CONSUMER_SECRET)
  val nonstop:Site = Site("nonstop",Nonstop.URL,Nonstop.CONSUMER_KEY,Nonstop.CONSUMER_SECRET)
}

object herokuComp {
  val client: FTP = FTPClient() // create a new FTP client instance

  def uploadFileWithBackup(resource: File)(site: Site, path:String = "", uploadFileName: Option[String] = None) : Unit = {

    uploadFile(resource)(site,path,uploadFileName)

    //val backupFileName = s"${site.name}_${Utils.getDateString()}.json.gz"
    uploadFile(resource)(site,s"${path}/backup", Some(FileUtils.addDateToFileName(resource)))

  }

  def uploadFile(resource: File)(site: Site, path:String = "", uploadFileName: Option[String] = None) : Unit = {

    //Connect
    client.connectWithAuth(Nonstop.HOST, Nonstop.KULLANICIADI, Nonstop.PAS)

    //Open """ for uploading
    client.cd(path)

    //Upload file
    val fileStream: InputStream = resource.newFileInputStream
    try{
      client.uploadFile(uploadFileName.getOrElse(resource.name), fileStream)
    } catch {
      case e: IOException => e.printStackTrace
    } finally {
      client.disconnect()
    }
  }

}

object comp {

  def getItemRecordBySku(sku: String): HttpResponse[String] = {
    val get_url = "" + sku
    val get = Http(get_url)
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    get
  }

  def getOrders(): HttpResponse[String] = {
    val orders = Http("").auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    orders
  }

  def getAllProducts(site:Site) = {
    val totalPages = getProductPageCount()(site)
    val futures: IndexedSeq[Future[List[Json]]] = (1 to totalPages) map (i => {
      Future {
        getProductsAsJsonList(i)(site)
      }
    })
    val futureSeq: Future[IndexedSeq[List[Json]]] = Future.sequence(futures)
    val t: IndexedSeq[List[Json]] = Await.result(futureSeq, 1000 seconds)
    val merge: List[Json] = t.reduceLeft(_ ++ _)
    merge
  }

  def getAllProducts2(site:Site) = {
    val totalPages = getProductPageCount()(site)
    val futures: IndexedSeq[Future[List[Json]]] = (1 to totalPages) map (i => {
      Future {
        getProductsAsJsonList(i)(site)
      }
    })
    futures
  }

  def getProductsAsJsonList(page: Int = 1, per_page: Int = AppProperties.GET_PERPAGE_NUM)(site:Site): List[Json] = {
    val products = Http(s"${site.url}")
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(site.userId, site.pass).asString
    val json = responseToJSON(products)
    val list = rootJsonToList(json)
    list
  }

  def getProductCount(site:Site): Int = {
    val firstProduct = Http(s"${site.url}")
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(site.userId, site.pass).asString
    val header = firstProduct.headers.filter(_._1 == "X-WP-Total")
    toInt(header.head._2(0)).getOrElse(0)
  }

  def getProductPageCount(perPage: Int = AppProperties.GET_PERPAGE_NUM)(site:Site): Int = {
    val per_page = perPage
    val total = getProductCount(site)
    val totalPage = (total / per_page) + 1
    totalPage
  }

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  def getProductByID(id: Int): HttpResponse[String] = {
    val product = Http("" + id.toString).auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    product

  }

  def responseToJSON(response: HttpResponse[String]): Json = {
    val bytes: Array[Byte] = response.body.getBytes(StandardCharsets.UTF_8)
    val responseBody: String = response.body
    val json: Json = parse(responseBody).getOrElse(Json.Null)
    json
  }

  def rootJsonToList(rootJson: Json): List[Json] = {
    val items = root.each.json
    val itemListJSON: List[Json] = items.getAll(rootJson)
    itemListJSON
  }

  def getPublishedProducts(list:List[Json]) = {
    val statusOptics = root.status.string
    def getStatus(json:Json) = statusOptics.getOption(json)
    //list.foreach(x=>println(root.status.string.getOption(x).getOrElse("cantfind")))
    val filtered = list.filter(x=> getStatus(x).getOrElse("cantfind") == "publish")
    filtered
  }

  def saveJsonFile(jsonList:List[Json])(fileName:String) = {
    val jsonStr = jsonList.asJson.toString()
    val file = File(s"${fileName}_all.json")
    file.createIfNotExists()
    file.overwrite(jsonStr)
    file
  }


  def removeNullsFromJson(json: Json): String = {
    json.pretty(Printer.noSpaces.copy(dropNullValues = true))
  }

  val jsonstr = ""
  val jsonstr2 =  ""
  val jsonstr3 = ""
  val default_img = ""

}

object Utils {
  def addSpaces(str:String, num:Int) = {
    val space = (1 to num) map (p=> " ") mkString("")
    space + str + space
  }
  def getDateString():String = {
    new SimpleDateFormat("YYYYMMdd_HHmmss").format(new Date)
  }
  def md5HashString(s: String): String = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1,digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }
}

object FileUtils{
  def createFile(str:String)(fileName:String): File = {
    val file = File(s"${fileName}")
    file.createIfNotExists()
    file.overwrite(str)
    file
  }

  def addDateToFileName(file:File):String = {
    val splitted = file.name.split("\\.")
    val name = splitted.head
    val newName = s"${name}_${Utils.getDateString()}"
    return (List(newName) ++ splitted.drop(1)).mkString(".")
  }
}