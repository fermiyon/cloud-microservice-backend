package foo

import org.scalatra.test.scalatest._

class fooTests extends ScalatraFunSuite {

  addServlet(classOf[foo], "/*")

  test("GET / on foo should return status 200") {
    get("/") {
      status should equal (200)
    }
  }

}
