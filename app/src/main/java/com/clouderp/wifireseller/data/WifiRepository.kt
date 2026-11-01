package com.clouderp.wifireseller.data

class WifiRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
) {
    suspend fun login(email: String, password: String): LoginData {
        val data = unwrap { api.auth.login(LoginRequest(email.trim(), password)) }
        sessionStore.saveLogin(data.token, data.user?.name, data.user?.email)
        return data
    }

    suspend fun companies(): List<CompanyDto> =
        unwrap { api.companies.companies() }.data

    suspend fun selectCompany(company: CompanyDto) {
        sessionStore.saveCompany(company.uuid, company.name)
    }

    suspend fun bootstrap(): BootstrapData = unwrap { api.wifi.bootstrap() }

    suspend fun dashboard(): DashboardData = unwrap { api.wifi.dashboard() }

    suspend fun sales(): SalesData = unwrap { api.wifi.sales() }

    suspend fun live(refresh: Boolean = false): LiveMonitorData =
        unwrap { api.wifi.live(if (refresh) 1 else null) }

    suspend fun sites(): List<SiteDto> = unwrap { api.wifi.sites() }.data

    suspend fun site(id: Long): SiteShowResponse = unwrap { api.wifi.site(id) }

    suspend fun createSite(body: SiteUpsertRequest): SiteCreateResponse =
        unwrap { api.wifi.createSite(body) }

    suspend fun updateSite(id: Long, body: SiteUpsertRequest): SiteDto =
        unwrap { api.wifi.updateSite(id, body) }

    suspend fun rotateToken(id: Long): SiteCreateResponse =
        unwrap { api.wifi.rotateToken(id) }

    suspend fun deleteSite(id: Long) = unwrapUnit { api.wifi.deleteSite(id) }

    suspend fun plansIndex(): PlansIndexData = unwrap { api.wifi.plans() }

    suspend fun createPlan(body: PlanUpsertRequest): PlanDto =
        unwrap { api.wifi.createPlan(body) }

    suspend fun updatePlan(id: Long, body: PlanUpsertRequest): PlanDto =
        unwrap { api.wifi.updatePlan(id, body) }

    suspend fun deletePlan(id: Long) = unwrapUnit { api.wifi.deletePlan(id) }

    suspend fun vouchers(q: String? = null, status: String? = null): List<VoucherDto> =
        unwrap { api.wifi.vouchers(q, status) }.data

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

    suspend fun logout() {
        runCatching { unwrapUnit { api.auth.logout() } }
        sessionStore.clear()
    }
}
