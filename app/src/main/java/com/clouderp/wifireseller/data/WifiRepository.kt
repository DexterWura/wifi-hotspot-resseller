package com.clouderp.wifireseller.data

class WifiRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
) {
    suspend fun login(email: String, password: String): LoginData {
        val data = unwrap { api.auth.login(LoginRequest(email.trim(), password)) }
        sessionStore.saveLogin(data.token, data.user?.name, data.user?.email)
        api.syncAuth(data.token, null)
        try {
            bindWorkspace()
        } catch (e: Exception) {
            api.syncAuth(null, null)
            sessionStore.clear()
            throw e
        }
        return data
    }

    suspend fun bindWorkspace() {
        val company = unwrap { api.companies.companies() }.firstOrNull()
            ?: throw ApiException("No workspace is assigned to this account.")
        sessionStore.saveCompany(company.uuid, company.name)
        api.syncCompany(company.uuid)
    }

    suspend fun logout() {
        runCatching { unwrapUnit { api.auth.logout() } }
        api.syncAuth(null, null)
        sessionStore.clear()
    }

    suspend fun bootstrap(): BootstrapData = unwrap { api.wifi.bootstrap() }

    suspend fun dashboard(): DashboardData = unwrap { api.wifi.dashboard() }

    suspend fun sales(
        from: String? = null,
        to: String? = null,
        siteId: Long? = null,
        channel: String? = null,
        page: Int? = null,
    ): SalesData = unwrap { api.wifi.sales(from, to, siteId, channel, page) }

    suspend fun live(refresh: Boolean = false): LiveMonitorData =
        unwrap { api.wifi.live(if (refresh) 1 else null) }

    suspend fun sites(page: Int? = null): PaginatedSites = unwrap { api.wifi.sites(page) }

    suspend fun site(id: Long): SiteShowResponse = unwrap { api.wifi.site(id) }

    suspend fun createSite(body: SiteUpsertRequest): SiteCreateResponse =
        unwrap { api.wifi.createSite(body) }

    suspend fun updateSite(id: Long, body: SiteUpsertRequest): SiteDto =
        unwrap { api.wifi.updateSite(id, body) }

    suspend fun rotateToken(id: Long): SiteCreateResponse =
        unwrap { api.wifi.rotateToken(id) }

    suspend fun testConnection(id: Long): MessageData =
        unwrap { api.wifi.testConnection(id) }

    suspend fun syncSite(id: Long): MessageData =
        unwrap { api.wifi.syncSite(id) }

    suspend fun deleteSite(id: Long) = unwrapUnit { api.wifi.deleteSite(id) }

    suspend fun plansIndex(page: Int? = null): PlansIndexData = unwrap { api.wifi.plans(page) }

    suspend fun plan(id: Long): PlanDto = unwrap { api.wifi.plan(id) }

    suspend fun createPlan(body: PlanUpsertRequest): PlanDto =
        unwrap { api.wifi.createPlan(body) }

    suspend fun updatePlan(id: Long, body: PlanUpsertRequest): PlanDto =
        unwrap { api.wifi.updatePlan(id, body) }

    suspend fun issueOwnerVoucher(planId: Long, siteId: Long, phone: String?): VoucherDto =
        unwrap {
            api.wifi.issueOwnerVoucher(
                planId,
                OwnerVoucherRequest(siteId, phone?.takeIf { it.isNotBlank() }),
            )
        }

    suspend fun deletePlan(id: Long) = unwrapUnit { api.wifi.deletePlan(id) }

    suspend fun vouchers(q: String? = null, status: String? = null, page: Int? = null): PaginatedVouchers =
        unwrap { api.wifi.vouchers(q, status, page) }

    suspend fun voucher(id: Long): VoucherDto = unwrap { api.wifi.voucher(id) }

    suspend fun sellOptions(): SellOptionsData = unwrap { api.wifi.sellOptions() }

    suspend fun sell(siteId: Long, planId: Long, phone: String?): VoucherDto =
        unwrap {
            api.wifi.sell(
                SellRequest(
                    siteId = siteId,
                    planId = planId,
                    guestPhone = phone?.takeIf { it.isNotBlank() },
                ),
            )
        }

    suspend fun revoke(id: Long): VoucherDto = unwrap { api.wifi.revoke(id) }

    suspend fun settings(): SettingsDto = unwrap { api.wifi.settings() }

    suspend fun updateSettings(body: SettingsUpdateRequest): SettingsDto =
        unwrap { api.wifi.updateSettings(body) }
}
