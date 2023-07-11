package japgolly.webapputil.test.node

import japgolly.microlibs.testutil.TestUtil._
import japgolly.scalajs.react.AsyncCallback
import org.scalajs.dom.Crypto
import scala.concurrent.Future
import scala.scalajs.js

/** Node JS access provided by `project/AdvancedNodeJSEnv.scala`. */
trait TestNode {

  @inline def window = js.Dynamic.global.window
  @inline def node = window.node

  def require(path: String): js.Dynamic =
    node.require(path).asInstanceOf[js.Dynamic]

  def envVarGet(name: String): js.UndefOr[String] =
    node.process.env.selectDynamic(name).asInstanceOf[js.UndefOr[String]]

  def envVarNeed(name: String): String =
    envVarGet(name).getOrElse(throw new RuntimeException("Missing env var: " + name))

  val inCI             = envVarGet("CI").contains("1")
  var asyncTestTimeout = if (inCI) 60000 else 3000

  def asyncTest[A](timeoutMs: Int = asyncTestTimeout)(ac: AsyncCallback[A]): Future[A] =
    ac.timeoutMs(timeoutMs).map {
      case Some(a) => a
      case None    => fail(s"Async test timed out after ${timeoutMs / 1000} sec.")
    }.unsafeToFuture()

  lazy val webCrypto: Crypto = {
    // https://github.com/nodejs/node/pull/35093
    require("crypto").webcrypto.asInstanceOf[Crypto]
  }
}

object TestNode extends TestNode
