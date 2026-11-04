package com.clouderp.wifireseller.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginData>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit?>
}

interface CompanyApi {
    @GET("api/v1/companies")
    suspend fun companies(): ApiEnvelope<List<CompanyDto>>
}

interface WifiApi {
    @GET("api/v1/wifi/bootstrap")
    suspend fun bootstrap(): ApiEnvelope<BootstrapData>

    @GET("api/v1/wifi/dashboard")
    suspend fun dashboard(): ApiEnvelope<DashboardData>

    @GET("api/v1/wifi/sales")
    suspend fun sales(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("site_id") siteId: Long? = null,
        @Query("channel") channel: String? = null,
        @Query("page") page: Int? = null,
    ): ApiEnvelope<SalesData>

    @GET("api/v1/wifi/live")
    suspend fun live(@Query("refresh") refresh: Int? = null): ApiEnvelope<LiveMonitorData>

    @GET("api/v1/wifi/sites")
    suspend fun sites(@Query("page") page: Int? = null): ApiEnvelope<PaginatedSites>

    @GET("api/v1/wifi/sites/{id}")
    suspend fun site(@Path("id") id: Long): ApiEnvelope<SiteShowResponse>

    @POST("api/v1/wifi/sites")
    suspend fun createSite(@Body body: SiteUpsertRequest): ApiEnvelope<SiteCreateResponse>

    @PUT("api/v1/wifi/sites/{id}")
    suspend fun updateSite(@Path("id") id: Long, @Body body: SiteUpsertRequest): ApiEnvelope<SiteDto>

    @POST("api/v1/wifi/sites/{id}/rotate-token")
    suspend fun rotateToken(@Path("id") id: Long): ApiEnvelope<SiteCreateResponse>

    @POST("api/v1/wifi/sites/{id}/test-connection")
    suspend fun testConnection(@Path("id") id: Long): ApiEnvelope<MessageData>

    @POST("api/v1/wifi/sites/{id}/sync")
    suspend fun syncSite(@Path("id") id: Long): ApiEnvelope<MessageData>

    @DELETE("api/v1/wifi/sites/{id}")
    suspend fun deleteSite(@Path("id") id: Long): ApiEnvelope<Unit?>

    @GET("api/v1/wifi/plans")
    suspend fun plans(@Query("page") page: Int? = null): ApiEnvelope<PlansIndexData>

    @GET("api/v1/wifi/plans/{id}")
    suspend fun plan(@Path("id") id: Long): ApiEnvelope<PlanDto>

    @POST("api/v1/wifi/plans")
    suspend fun createPlan(@Body body: PlanUpsertRequest): ApiEnvelope<PlanDto>

    @PUT("api/v1/wifi/plans/{id}")
    suspend fun updatePlan(@Path("id") id: Long, @Body body: PlanUpsertRequest): ApiEnvelope<PlanDto>

    @POST("api/v1/wifi/plans/{id}/owner-voucher")
    suspend fun issueOwnerVoucher(
        @Path("id") id: Long,
        @Body body: OwnerVoucherRequest,
    ): ApiEnvelope<VoucherDto>

    @DELETE("api/v1/wifi/plans/{id}")
    suspend fun deletePlan(@Path("id") id: Long): ApiEnvelope<Unit?>

    @GET("api/v1/wifi/vouchers")
    suspend fun vouchers(
        @Query("q") q: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
    ): ApiEnvelope<PaginatedVouchers>

    @GET("api/v1/wifi/vouchers/{id}")
    suspend fun voucher(@Path("id") id: Long): ApiEnvelope<VoucherDto>

    @GET("api/v1/wifi/sell/options")
    suspend fun sellOptions(): ApiEnvelope<SellOptionsData>

    @POST("api/v1/wifi/sell")
    suspend fun sell(@Body body: SellRequest): ApiEnvelope<VoucherDto>

    @POST("api/v1/wifi/vouchers/{id}/revoke")
    suspend fun revoke(@Path("id") id: Long): ApiEnvelope<VoucherDto>

    @GET("api/v1/wifi/settings")
    suspend fun settings(): ApiEnvelope<SettingsDto>

    @PUT("api/v1/wifi/settings")
    suspend fun updateSettings(@Body body: SettingsUpdateRequest): ApiEnvelope<SettingsDto>
}
