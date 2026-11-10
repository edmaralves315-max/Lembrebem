package org.lembrebem.clinical

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private app storage + non-exportable AndroidKeyStore AES-GCM. No backup/remote upload. */
class ClinicalStorage(context: Context) {
    private val directory = File(context.noBackupFilesDir, "clinical").apply { mkdirs() }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("lembrebem.clinical.v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("lembrebem.clinical.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(id: String, content: ByteArray) {
        require(id.matches(Regex("[a-zA-Z0-9_.-]+")))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(content)
        val target = File(directory, id)
        val temporary = File(directory, "$id.part")
        temporary.outputStream().use { it.write(cipher.iv.size); it.write(cipher.iv); it.write(encrypted) }
        check(temporary.renameTo(target))
    }
    fun read(id: String): ByteArray {
        require(id.matches(Regex("[a-zA-Z0-9_.-]+")))
        val bytes = File(directory, id).readBytes()
        val size = bytes[0].toInt() and 255
        require(size == 12 && bytes.size > size + 17)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, size + 1)))
            doFinal(bytes.copyOfRange(size + 1, bytes.size))
        }
    }
    fun saveDraft(draft: ConsultationDraft) {
        val json = JSONObject().put("id", draft.id).put("createdAt", draft.createdAt)
            .put("source", draft.source).put("status", draft.status)
        save("draft-${draft.id}.json.enc", json.toString().toByteArray())
    }
    fun saveWorkingText(text: String) = save("working.txt.enc", text.toByteArray())
    fun workingText(): String = if (File(directory, "working.txt.enc").exists()) read("working.txt.enc").toString(Charsets.UTF_8) else ""
    fun pruneAudio(retentionDays: Int) {
        require(retentionDays in 1..365)
        val cutoff = Instant.now().minusSeconds(retentionDays * 86400L).toEpochMilli()
        directory.listFiles()?.filter { it.name.startsWith("audio-") && it.lastModified() < cutoff }?.forEach { it.delete() }
    }
    fun audioIds(): List<String> = directory.listFiles()?.filter { it.name.startsWith("audio-") && it.name.endsWith(".enc") }?.sortedBy { it.lastModified() }?.map { it.name } ?: emptyList()
    fun deleteAll() { directory.listFiles()?.forEach { it.delete() } }
}
