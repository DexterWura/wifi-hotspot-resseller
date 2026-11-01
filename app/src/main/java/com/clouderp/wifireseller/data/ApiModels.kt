package com.clouderp.wifireseller.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val errors: JsonElement? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "wifi-reseller-android",
)

@Serializable
data class LoginData(
    val token: String,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
)

@Serializable
data class CompanyDto(
    val id: Long? = null,
    val uuid: String,
    val name: String,
    val subdomain: String? = null,
)

@Serializable
data class BootstrapData(
    val company: CompanyDto,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class DashboardData(
    val earningsToday: List<MoneyTotal> = emptyList(),
    val earningsMonth: List<MoneyTotal> = emptyList(),
    val earningsAllTime: List<MoneyTotal> = emptyList(),
    val activeVouchers: Int = 0,
    val pendingSync: Int = 0,
    val vouchersSoldToday: Int = 0,
    val vouchersSoldMonth: Int = 0,
    val sites: List<SiteSummary> = emptyList(),
)

@Serializable
data class MoneyTotal(
    @SerialName("currency_code") val currencyCode: String = "USD",
    val total: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class SiteSummary(
    val id: Long,
    val name: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

@Serializable
data class SiteDto(
    val id: Long? = null,
    val name: String,
    val slug: String? = null,
    val ssid: String? = null,
    @SerialName("hotspot_server") val hotspotServer: String? = null,
    @SerialName("hotspot_login_url") val hotspotLoginUrl: String? = null,
    @SerialName("api_host") val apiHost: String? = null,
    @SerialName("api_port") val apiPort: Int? = null,
    @SerialName("api_use_tls") val apiUseTls: Boolean = true,
    @SerialName("api_username") val apiUsername: String? = null,
    @SerialName("api_password_set") val apiPasswordSet: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("vouchers_count") val vouchersCount: Int? = null,
    @SerialName("sessions_count") val sessionsCount: Int? = null,
)

@Serializable
data class SiteCreateResponse(
    val site: SiteDto,
    @SerialName("plain_token") val plainToken: String? = null,
)

@Serializable
data class SiteShowResponse(
    val site: SiteDto,
    @SerialName("sync_url") val syncUrl: String? = null,
    @SerialName("notice_url") val noticeUrl: String? = null,
)

@Serializable
data class SiteUpsertRequest(
    val name: String,
    val slug: String? = null,
    val ssid: String? = null,
    @SerialName("hotspot_server") val hotspotServer: String? = null,
    @SerialName("hotspot_login_url") val hotspotLoginUrl: String? = null,
    @SerialName("api_host") val apiHost: String? = null,
    @SerialName("api_port") val apiPort: Int? = null,
    @SerialName("api_use_tls") val apiUseTls: Boolean = true,
    @SerialName("api_username") val apiUsername: String? = null,
    @SerialName("api_password") val apiPassword: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class PlanDto(
    val id: Long? = null,
    val name: String,
    val price: Double = 0.0,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("duration_minutes") val durationMinutes: Int = 60,
    @SerialName("duration_label") val durationLabel: String? = null,
    @SerialName("data_limit_bytes") val dataLimitBytes: Long? = null,
    @SerialName("data_limit_label") val dataLimitLabel: String? = null,
    @SerialName("rate_limit") val rateLimit: String? = null,
    @SerialName("shared_users") val sharedUsers: Int = 1,
    @SerialName("mikrotik_profile") val mikrotikProfile: String? = null,
    @SerialName("site_id") val siteId: Long? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class PlansIndexData(
    val plans: PaginatedPlans? = null,
    val sites: List<SiteSummary> = emptyList(),
    val currencies: List<CurrencyDto> = emptyList(),
)

@Serializable
data class PaginatedPlans(
    val data: List<PlanDto> = emptyList(),
)

@Serializable
data class CurrencyDto(
    val code: String,
    val name: String? = null,
)

@Serializable
data class PlanUpsertRequest(
    val name: String,
    val price: Double,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("data_limit_bytes") val dataLimitBytes: Long? = null,
    @SerialName("rate_limit") val rateLimit: String? = null,
    @SerialName("shared_users") val sharedUsers: Int = 1,
    @SerialName("mikrotik_profile") val mikrotikProfile: String? = null,
    @SerialName("site_id") val siteId: Long? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class VoucherDto(
    val id: Long,
    val code: String,
    val status: String,
    val channel: String? = null,
    val amount: Double = 0.0,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("guest_phone") val guestPhone: String? = null,
    @SerialName("activated_at") val activatedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val site: NamedRef? = null,
    val plan: NamedRef? = null,
)

@Serializable
data class NamedRef(
    val id: Long? = null,
    val name: String? = null,
    val slug: String? = null,
    @SerialName("duration_label") val durationLabel: String? = null,
    @SerialName("data_limit_label") val dataLimitLabel: String? = null,
)

@Serializable
data class SellOptionsData(
    val sites: List<SiteSummary> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
)

@Serializable
data class SellRequest(
    @SerialName("site_id") val siteId: Long,
    @SerialName("plan_id") val planId: Long,
    @SerialName("guest_phone") val guestPhone: String? = null,
)

@Serializable
data class SettingsDto(
    @SerialName("paynow_enabled") val paynowEnabled: Boolean = false,
    @SerialName("paynow_integration_id") val paynowIntegrationId: String? = null,
    @SerialName("paynow_integration_key_set") val paynowIntegrationKeySet: Boolean = false,
    @SerialName("paynow_configured") val paynowConfigured: Boolean = false,
    @SerialName("pesepay_enabled") val pesepayEnabled: Boolean = false,
    @SerialName("pesepay_configured") val pesepayConfigured: Boolean = false,
    @SerialName("ecocash_enabled") val ecocashEnabled: Boolean = false,
    @SerialName("ecocash_configured") val ecocashConfigured: Boolean = false,
    @SerialName("guest_browser_notices_enabled") val guestBrowserNoticesEnabled: Boolean = true,
    @SerialName("guest_sms_warnings_enabled") val guestSmsWarningsEnabled: Boolean = false,
    @SerialName("guest_warning_percent") val guestWarningPercent: Int = 80,
    @SerialName("guest_expiry_warn_minutes") val guestExpiryWarnMinutes: Int = 15,
)

@Serializable
data class SettingsUpdateRequest(
    @SerialName("paynow_enabled") val paynowEnabled: Boolean? = null,
    @SerialName("paynow_integration_id") val paynowIntegrationId: String? = null,
    @SerialName("paynow_integration_key") val paynowIntegrationKey: String? = null,
    @SerialName("pesepay_enabled") val pesepayEnabled: Boolean? = null,
    @SerialName("ecocash_enabled") val ecocashEnabled: Boolean? = null,
    @SerialName("guest_browser_notices_enabled") val guestBrowserNoticesEnabled: Boolean? = null,
    @SerialName("guest_sms_warnings_enabled") val guestSmsWarningsEnabled: Boolean? = null,
    @SerialName("guest_warning_percent") val guestWarningPercent: Int? = null,
    @SerialName("guest_expiry_warn_minutes") val guestExpiryWarnMinutes: Int? = null,
)

@Serializable
data class PaginatedCompanies(
    val data: List<CompanyDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
)

@Serializable
data class PaginatedSites(
    val data: List<SiteDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
)

@Serializable
data class PaginatedVouchers(
    val data: List<VoucherDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
)

@Serializable
data class SalesData(
    val from: String? = null,
    val to: String? = null,
    val totals: List<MoneyTotal> = emptyList(),
    @SerialName("voucherCount") val voucherCount: Int = 0,
    val rows: SalesRows? = null,
)

@Serializable
data class SalesRows(
    val data: List<VoucherDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class LiveMonitorData(
    @SerialName("refreshed_at") val refreshedAt: String? = null,
    @SerialName("connected_count") val connectedCount: Int = 0,
    @SerialName("active_vouchers") val activeVouchers: Int = 0,
    @SerialName("total_rate_bps") val totalRateBps: Long = 0,
    @SerialName("total_rate_label") val totalRateLabel: String = "0 kbps",
    @SerialName("total_bytes_in") val totalBytesIn: Long = 0,
    @SerialName("total_bytes_out") val totalBytesOut: Long = 0,
    @SerialName("total_bytes_label") val totalBytesLabel: String = "0 B",
    val sites: List<LiveSiteRow> = emptyList(),
    val sessions: List<LiveSessionRow> = emptyList(),
)

@Serializable
data class LiveSiteRow(
    @SerialName("site_id") val siteId: Long? = null,
    val name: String = "",
    val connected: Int = 0,
    @SerialName("total_rate_bps") val totalRateBps: Long = 0,
    @SerialName("bytes_in") val bytesIn: Long = 0,
    @SerialName("bytes_out") val bytesOut: Long = 0,
)

@Serializable
data class LiveSessionRow(
    val username: String = "",
    @SerialName("mac_address") val macAddress: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    val uptime: String? = null,
    @SerialName("idle_time") val idleTime: String? = null,
    @SerialName("login_by") val loginBy: String? = null,
    @SerialName("bytes_in") val bytesIn: Long = 0,
    @SerialName("bytes_out") val bytesOut: Long = 0,
    @SerialName("bytes_total") val bytesTotal: Long = 0,
    @SerialName("bytes_total_label") val bytesTotalLabel: String = "0 B",
    @SerialName("packets_in") val packetsIn: Long? = null,
    @SerialName("packets_out") val packetsOut: Long? = null,
    @SerialName("rate_bps") val rateBps: Long = 0,
    @SerialName("rate_label") val rateLabel: String = "0 kbps",
    @SerialName("reported_at") val reportedAt: String? = null,
    @SerialName("is_stale") val isStale: Boolean = false,
    val site: NamedRef? = null,
    val voucher: LiveVoucherRef? = null,
    val plan: LivePlanRef? = null,
)

@Serializable
data class LiveVoucherRef(
    val id: Long? = null,
    val status: String? = null,
    val channel: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class LivePlanRef(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("rate_limit") val rateLimit: String? = null,
    @SerialName("data_limit_label") val dataLimitLabel: String? = null,
    @SerialName("duration_label") val durationLabel: String? = null,
)
