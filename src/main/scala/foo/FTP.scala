package foo

import org.apache.commons.net.ftp._
import scala.util.Try
import scala.io.Source.fromInputStream
import java.io.{File, FileOutputStream, InputStream}

final class FTP(client: FTPClient) {
  def login(username: String, password: String): Try[Boolean] = Try {
    client.login(username, password)
  }

  def connect(host: String): Try[Unit] = Try {
    client.connect(host)
    client.enterLocalPassiveMode()
  }

  def connected: Boolean = client.isConnected
  def disconnect(): Unit = client.disconnect()

  def canConnect(host: String): Boolean = {
    client.connect(host)
    val connectionWasEstablished = connected
    client.disconnect()
    connectionWasEstablished
  }

  def listFiles(dir: Option[String]): Array[FTPFile] = dir match {
    case Some(d) => client.listFiles(d)
    case None    => client.listFiles
  }

  def connectWithAuth(host: String,
                      username: String = "anonymous",
                      password: String = "") : Try[Boolean] = {
    for {
      connection <- connect(host)
      login      <- login(username, password)
    } yield login
  }

  def extractNames(f: Option[String] => Array[FTPFile]) =
    f(None).map(_.getName).toSeq

  def cd(path: String): Boolean =
    client.changeWorkingDirectory(path)

  def filesInCurrentDirectory: Seq[String] =
    extractNames(listFiles)

  def downloadFileStream(remote: String): InputStream = {
    val stream = client.retrieveFileStream(remote)
    client.completePendingCommand() // make sure it actually completes!!
    stream
  }

  def downloadFile(remote: String): Boolean = {
    val os = new FileOutputStream(new File(remote))
    client.retrieveFile(remote, os)
  }

  def streamAsString(stream: InputStream): String =
    fromInputStream(stream).mkString

  def uploadFile(remote: String, input: InputStream): Boolean = {
    client.setFileType(FTP.BINARY_FILE_TYPE)
    client.storeFile(remote, input)
  }



}