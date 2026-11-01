package com.clouderp.wifireseller

import android.app.Application
import com.clouderp.wifireseller.data.ApiClient
import com.clouderp.wifireseller.data.SessionStore
import com.clouderp.wifireseller.data.WifiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WifiResellerApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var repository: WifiRepository
        private set

    private val tokenState = MutableStateFlow<String?>(null)
    private val companyState = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        runBlocking {
            val snap = sessionStore.session.first()
            tokenState.value = snap.token
            companyState.value = snap.companyUuid
        }
        val api = ApiClient(
            tokenProvider = { tokenState.value },
            companyProvider = { companyState.value },
        )
        repository = WifiRepository(api, sessionStore)
    }

    fun syncAuth(token: String?, companyUuid: String?) {
        tokenState.value = token
        companyState.value = companyUuid
    }
}
