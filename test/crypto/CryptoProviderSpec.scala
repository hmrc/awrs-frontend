/*
 * Copyright 2025 HM Revenue & Customs
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

package crypto

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.crypto.{PlainText, SymmetricCryptoFactory}

class CryptoProviderSpec extends PlaySpec {

  private val gcmKeyA = "/d2Wt03HLJwVHKA1cDAQ+iLR/yk5kUzack1eCaojp88="
  private val gcmKeyB = "uDmNVE2rpGhSJLF/COYEt0YyF6Uef4QX5NAqjrF8hlU="
  private val ecbKeyA = "VGTpP4TjWR4DShgEiD2caSKeLzdSbbf7ZCJ2e2d9EqE="
  private val ecbKeyB = "/CAzziSQUFcKBQNyLo4JdwGd9QSIoKygStoU6S4SyqA="
  private val ecbKeyOld = "QO3buX9y0wvYeYFET0/Prc5/lOOik7yO0cP62CTCx2Y="

  private def cfgWith(gcmKey: String, ecbKey: String, ecbPrevious: Seq[String] = Nil): Configuration = {
    val prev = ecbPrevious.map(k => s""""$k"""").mkString(", ")
    val hocon =
      s"""
         |json.encryption {
         |  enabled = true
         |  key = "$ecbKey"
         |  previousKeys = [ $prev ]
         |}
         |json.encryptionGcm {
         |  key = "$gcmKey"
         |  previousKeys = []
         |}
         |""".stripMargin
    Configuration(ConfigFactory.parseString(hocon))
  }

  private def legacyEcbCrypto(cfg: Configuration) =
    SymmetricCryptoFactory.aesCryptoFromConfig("json.encryption", cfg.underlying)

  "CryptoProvider" should {

    "encrypt and decrypt (round-trip) new values via AES-GCM" in {
      val provider = new CryptoProvider(cfgWith(gcmKeyA, ecbKeyA))
      val crypto   = provider.crypto
      val plain    = "Hello £Ü 𐍈 — {\"a\":1}"
      val enc      = crypto.encrypt(PlainText(plain))
      enc.value must not equal plain
      crypto.decrypt(enc).value mustBe plain
    }

    "decrypt legacy ECB-encrypted values via the fallback" in {
      val cfg      = cfgWith(gcmKeyA, ecbKeyA)
      val existing = legacyEcbCrypto(cfg).encrypt(PlainText("legacy-cache-entry"))
      val provider = new CryptoProvider(cfg)
      provider.crypto.decrypt(existing).value mustBe "legacy-cache-entry"
    }

    "write new values as AES-GCM, not legacy ECB" in {
      val cfg       = cfgWith(gcmKeyA, ecbKeyA)
      val gcmCipher = new CryptoProvider(cfg).crypto.encrypt(PlainText("new-cache-entry"))
      an [SecurityException] must be thrownBy legacyEcbCrypto(cfg).decrypt(gcmCipher)
    }

    "support rotation via ECB previousKeys (new provider reads old ciphertext)" in {
      val ciphertext  = legacyEcbCrypto(cfgWith(gcmKeyA, ecbKeyOld)).encrypt(PlainText("rotate-me"))
      val newProvider = new CryptoProvider(cfgWith(gcmKeyA, ecbKeyA, ecbPrevious = Seq(ecbKeyOld)))
      newProvider.crypto.decrypt(ciphertext).value mustBe "rotate-me"
    }

    "round-trip GCM values independently of the ECB key in config" in {
      val enc    = new CryptoProvider(cfgWith(gcmKeyA, ecbKeyA)).crypto.encrypt(PlainText("gcm-only"))
      val reread = new CryptoProvider(cfgWith(gcmKeyA, ecbKeyB))
      reread.crypto.decrypt(enc).value mustBe "gcm-only"
    }

    "fail to decrypt when neither the GCM key nor any ECB key matches" in {
      val enc   = new CryptoProvider(cfgWith(gcmKeyA, ecbKeyA)).crypto.encrypt(PlainText("secret"))
      val other = new CryptoProvider(cfgWith(gcmKeyB, ecbKeyB))
      an [SecurityException] must be thrownBy other.crypto.decrypt(enc)
    }
  }
}