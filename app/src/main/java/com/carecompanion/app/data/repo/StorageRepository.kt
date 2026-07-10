package com.carecompanion.app.data.repo

import com.carecompanion.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Uploads images to Supabase Storage. Path convention: '{elderId}/{uuid}.jpg'.
 *  Buckets are public-read, so the returned public URL loads directly in Coil. */
@Singleton
class StorageRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    companion object {
        const val BUCKET_PHOTOS = "photos"
        const val BUCKET_MEDICINE = "medicine-images"
    }

    suspend fun upload(bucket: String, elderId: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val path = "$elderId/${UUID.randomUUID()}.jpg"
            val base = BuildConfig.SUPABASE_URL.trimEnd('/')
            val request = Request.Builder()
                .url("$base/storage/v1/object/$bucket/$path")
                .header("x-upsert", "true")
                .post(bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("upload failed ${resp.code}: ${resp.body?.string()}")
            }
            "$base/storage/v1/object/public/$bucket/$path"
        }
}
