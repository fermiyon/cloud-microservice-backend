package foo

import org.apache.commons.net.ftp.{FTPClient => ApacheFTPClient}

object FTPClient {
  def apply (): FTP =
    new FTP(new ApacheFTPClient)
}