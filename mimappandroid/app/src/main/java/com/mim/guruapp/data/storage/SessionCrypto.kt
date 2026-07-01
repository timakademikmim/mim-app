package com.mim.guruapp.data.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionCrypto {
  private val keyAlias = "mim_guru_auth_session_v1"
  private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

  fun encrypt(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
      val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
      val payload = ByteArray(1 + cipher.iv.size + encrypted.size)
      payload[0] = cipher.iv.size.toByte()
      cipher.iv.copyInto(payload, destinationOffset = 1)
      encrypted.copyInto(payload, destinationOffset = 1 + cipher.iv.size)
      Base64.encodeToString(payload, Base64.NO_WRAP)
    }.getOrDefault("")
  }

  fun decrypt(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
      val payload = Base64.decode(value, Base64.NO_WRAP)
      val ivSize = payload.firstOrNull()?.toInt()?.and(0xff) ?: return@runCatching ""
      if (ivSize !in 12..16 || payload.size <= 1 + ivSize) return@runCatching ""
      val iv = payload.copyOfRange(1, 1 + ivSize)
      val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
      cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrDefault("")
  }

  private fun getOrCreateKey(): SecretKey {
    val existing = keyStore.getKey(keyAlias, null) as? SecretKey
    if (existing != null) return existing

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }
}
