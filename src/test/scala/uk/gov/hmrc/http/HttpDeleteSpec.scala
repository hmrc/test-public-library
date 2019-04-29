/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.http

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpDeleteSpec extends WordSpecLike with Matchers with MockitoSugar with CommonHttpBehaviour {

  class StubbedHttpDelete(response: Future[HttpResponse]) extends HttpDelete with ConnectionTracingCapturing {
    val testHook1                                   = mock[HttpHook]
    val testHook2                                   = mock[HttpHook]
    val hooks                                       = Seq(testHook1, testHook2)
    override def configuration: Option[Config]      = None
    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")

    def appName: String                                   = ???
    def doDelete(url: String)(implicit hc: HeaderCarrier) = response
  }

  "HttpDelete" should {
    "be able to return plain responses" in {
      val response   = new DummyHttpResponse(testBody, 200)
      val testDelete = new StubbedHttpDelete(Future.successful(response))
      testDelete.DELETE(url).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testDelete
        .DELETE[TestClass](url)
        .futureValue(Timeout(Span(2, Seconds)), Interval(Span(15, Millis))) shouldBe TestClass("t", 10)
    }
    behave like anErrorMappingHttpCall("DELETE", (url, responseF) => new StubbedHttpDelete(responseF).DELETE(url))
    behave like aTracingHttpCall("DELETE", "DELETE", new StubbedHttpDelete(defaultHttpResponse)) { _.DELETE(url) }

    "Invoke any hooks provided" in {
      val dummyResponse       = new DummyHttpResponse(testBody, 200)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testDelete          = new StubbedHttpDelete(dummyResponseFuture)

      testDelete.DELETE(url).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testDelete.testHook1).apply(is(url), is("DELETE"), is(None), respArgCaptor1.capture())(any(), any())
      verify(testDelete.testHook2).apply(is(url), is("DELETE"), is(None), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }
}
