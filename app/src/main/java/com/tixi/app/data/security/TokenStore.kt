package com.tixi.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MinerU Token 的本地加密存储。
 *
 * - 密钥由 AndroidKeyStore 托管（别名 [KEY_ALIAS]），不落盘、不可导出；
 * - 文件位于 noBackupFilesDir/token.enc，不参与备份；
 * - 文件格式：[1 byte 版本=1][12 byte IV][GCM 密文]；
 * - 任何路径都不得把 token 打进日志（本类刻意不引用 Log）。
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var cachedHasToken: Boolean? = null

    private val lock = Any()

    private val tokenFile: File
        get() = File(context.noBackupFilesDir, FILE_NAME)

    /** 未配置 / 解密失败均返回 null；解密失败会顺手清理损坏文件 */
    fun read(): String? {
        synchronized(lock) {
            val file = tokenFile
            if (!file.exists()) {
                cachedHasToken = false
                return null
            }
            val payload = try {
                AtomicFile(file).readFully()
            } catch (e: IOException) {
                deleteFiles()
                cachedHasToken = false
                return null
            }
            if (payload.size <= 1 + IV_LENGTH || payload[0] != VERSION) {
                deleteFiles()
                cachedHasToken = false
                return null
            }
            val iv = payload.copyOfRange(1, 1 + IV_LENGTH)
            val body = payload.copyOfRange(1 + IV_LENGTH, payload.size)
            return try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
                val plain = cipher.doFinal(body)
                val token = String(plain, Charsets.UTF_8)
                cachedHasToken = token.isNotBlank()
                token.takeIf { it.isNotBlank() }
            } catch (e: GeneralSecurityException) {
                deleteFiles()
                cachedHasToken = false
                null
            } catch (e: IOException) {
                deleteFiles()
                cachedHasToken = false
                null
            } catch (e: RuntimeException) {
                deleteFiles()
                cachedHasToken = false
                null
            }
        }
    }

    /** 覆写保存；空白 token 等价于清除。写入失败不抛出，只是保持「未配置」状态 */
    fun write(token: String) {
        synchronized(lock) {
            if (token.isBlank()) {
                deleteFiles()
                cachedHasToken = false
                return
            }
            val atomic = AtomicFile(tokenFile)
            var stream: FileOutputStream? = null
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey())
                val iv = cipher.iv
                val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

                val payload = ByteArray(1 + iv.size + encrypted.size)
                payload[0] = VERSION
                System.arraycopy(iv, 0, payload, 1, iv.size)
                System.arraycopy(encrypted, 0, payload, 1 + iv.size, encrypted.size)

                stream = atomic.startWrite()
                stream.write(payload)
                stream.flush()
                atomic.finishWrite(stream)
                cachedHasToken = true
            } catch (e: GeneralSecurityException) {
                stream?.let { atomic.failWrite(it) }
                deleteFiles()
                cachedHasToken = false
            } catch (e: IOException) {
                stream?.let { atomic.failWrite(it) }
                deleteFiles()
                cachedHasToken = false
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            deleteFiles()
            cachedHasToken = false
        }
    }

    fun hasToken(): Boolean = cachedHasToken ?: (read() != null)

    private fun deleteFiles() {
        val base = tokenFile
        runCatching { base.delete() }
        runCatching { File(base.path + ".bak").delete() }
    }

    /** 首次调用时生成 AES-256-GCM 密钥并按别名存入 AndroidKeyStore */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "tixi_token_key"
        const val FILE_NAME = "token.enc"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        const val VERSION: Byte = 1
    }
}
