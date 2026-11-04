package com.clouderp.wifireseller.data

import com.clouderp.wifireseller.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ApiClient(
    private val tokenProvider: () -> String?,
    private val companyProvider: () -> String?,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val tokenOverride = AtomicReference<String?>(null)
    private val companyOverride = AtomicReference<String?>(null)

    fun syncAuth(token: String?, companyUuid: String?) {
        tokenOverride.set(token)
        companyOverride.set(companyUuid)
    }

    fun syncCompany(companyUuid: String?) {
        companyOverride.set(companyUuid)
    }

    private fun currentToken(): String? = tokenOverride.get() ?: tokenProvider()

    private fun currentCompany(): String? = companyOverride.get() ?: companyProvider()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
            currentToken()?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
            currentCompany()?.takeIf { it.isNotBlank() }?.let {
                builder.header("X-Company-Id", it)
            }
            chain.proceed(builder.build())
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val auth: AuthApi = retrofit.create(AuthApi::class.java)
    val companies: CompanyApi = retrofit.create(CompanyApi::class.java)
    val wifi: WifiApi = retrofit.create(WifiApi::class.java)
}

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

suspend fun <T> unwrap(block: suspend () -> ApiEnvelope<T>): T {
    val envelope = try {
        block()
    } catch (e: retrofit2.HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
            ?: e.message()
            ?: "Request failed"
        throw ApiException(message, e.code())
    } catch (e: Exception) {
        throw ApiException(e.message ?: "Network error")
    }

    if (!envelope.success || envelope.data == null) {
        throw ApiException(envelope.message ?: "Request failed")
    }
    return envelope.data
}

suspend fun unwrapUnit(block: suspend () -> ApiEnvelope<Unit?>) {
    val envelope = try {
        block()
    } catch (e: retrofit2.HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
            ?: e.message()
            ?: "Request failed"
        throw ApiException(message, e.code())
    } catch (e: Exception) {
        throw ApiException(e.message ?: "Network error")
    }
    if (!envelope.success) {
        throw ApiException(envelope.message ?: "Request failed")
    }
}
