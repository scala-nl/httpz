package httpz.native

import java.io.ByteArrayInputStream
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.After
import httpz.native.Http._
import com.github.kristofa.test.http._

class HttpTest {

  val port = 51234
  val url  = "http://localhost:" + port

  val rProvider = new SimpleHttpResponseProvider()
  val server    = new MockHttpServer(port, rProvider)

  val cType    = "text/html"
  val rCode    = 200
  val response = "<html>ok</html>"

  @Before
  def setUp(): Unit = {
    server.start()
  }

  @After
  def tearDown(): Unit = {
    server.stop()
  }

  @Test
  def asCodeHeaders(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, response);

    val (code, headers) = Http(url).asCodeHeaders
    assertTrue(headers.contains("Content-Type"))
    assertEquals(code, rCode)
  }

  @Test
  def asParams(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, "foo=bar");

    val result = Http(url).asParams
    assertEquals(result, List(("foo", "bar")))
  }

  @Test
  def asParamMap(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, "foo=bar");

    val result = Http(url).asParamMap
    assertEquals(result, Map("foo" -> "bar"))
  }

  @Test
  def asBytes(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, response);

    val result = Http(url).asBytes
    assertNotNull(result)
  }

  @Test
  def forceCharset(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, response);

    val result = Http(url).charset("ISO-8859-1").asString
    assertNotNull("the result should not be null", result)
  }


  @Test
  def shouldPrependOptions(): Unit = {
    val http = Http(url)
    val origOptions = http.options
    val origOptionsLength = origOptions.length
    val newOptions: List[HttpOptions.HttpOption] = List(c => { }, c=> { }, c => {})
    val http2 = http.options(newOptions)

    assertEquals(http2.options.length, origOptionsLength + 3)
    assertEquals(http2.options.take(3), newOptions)
    assertEquals(origOptions.length, origOptionsLength)
  }

  @Test
  def lastTimeoutValueShouldWin(): Unit = {
    rProvider.expect(Method.GET, "/").respondWith(rCode, cType, response);

    val getFunc: HttpExec = (req,conn) => {

    }

    val r = Request(getFunc, Http.noopHttpUrl(url), "GET")
      .options(HttpOptions.connTimeout(1234)).options(HttpOptions.readTimeout(1234))
    r.process(c => {
      assertEquals(c.getReadTimeout, 1234)
      assertEquals(c.getConnectTimeout, 1234)
    })
  }

  @Test
  def readString(): Unit = {
    val bais = new ByteArrayInputStream("hello there".getBytes(Http.utf8))
    assertEquals("hello there", Http.readString(bais))
  }

  @Test
  def overrideTheMethod(): Unit = {
    rProvider.expect(Method.DELETE, "/").respondWith(rCode, cType, response)
    val req = Http(url).method("DELETE")
    req.process(c => {
      assertEquals("DELETE", c.getRequestMethod)
      Http.readString(c.getInputStream())
    })
  }

  @Test
  def allModificationsAreAdditive() {
    val params = List("a" -> "b")
    val headers = List("foo" -> "bar")
    val options = List(HttpOptions.readTimeout(1234))

    var req = Http(url).params(params)

    assertEquals("params", params, req.params)

    val expectedNewOptions = options ++ req.options
    req = req.options(options)

    assertEquals("params", params, req.params)
    assertEquals("options", expectedNewOptions, req.options)

    req = req.headers(headers)

    assertEquals("params", params, req.params)
    assertEquals("options", expectedNewOptions, req.options)
    assertEquals("headers", headers, req.headers)

  }
}
