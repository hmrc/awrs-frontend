package caching

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import utils.AwrsUnitTestTraits

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ShortLivedCacheSpec extends AwrsUnitTestTraits {

  val mockCacheRepo = mock[MongoCacheRepository[String]]
  val testCacheItem = CacheItem("TestCacheId", JsObject.apply(Seq("FormId" -> Json.parse("""{"value": "Data"}"""))),Instant.now(),Instant.now())
  val shortLivedCache = new ShortLivedCache(mongoComponent = mock[MongoComponent],
    collectionName = "test-cache",
    ttl = Duration(1, TimeUnit.SECONDS),
    timestampSupport = mock[TimestampSupport]) {
    override implicit val crypto: Decrypter with Encrypter = TestCrypto
    override lazy val cacheRepo: MongoCacheRepository[String] = mockCacheRepo
  }

  "ShortLivedCache" must {
    "save to cache correctly" in {
      when(mockCacheRepo.put[String]("TestCacheId")(DataKey("FormId"), "Data"))
        .thenReturn(Future.successful(testCacheItem))
      val result: CacheMap = await(shortLivedCache.cache[String]("TestCacheId", "FormId", "Data"))

      result.getEntry[String]("FormId") mustBe Some("Data")
    }



  }
}

object TestCrypto extends Encrypter with Decrypter {

  override def encrypt(value: PlainContent): Crypted =
    value match {
      case PlainText(text) => Crypted(text)
      case _               => throw new RuntimeException(s"Unable to encrypt unknown message type: $value")
    }

  override def decrypt(crypted: Crypted): PlainText =
    PlainText(crypted.value)

  override def decryptAsBytes(crypted: Crypted): PlainBytes =
    PlainBytes(crypted.value.getBytes)
}