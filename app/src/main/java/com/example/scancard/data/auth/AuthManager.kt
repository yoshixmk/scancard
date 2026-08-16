package com.example.scancard.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    
    private val _hfToken = MutableStateFlow(prefs.getString("hf_token", null))
    val hfToken = _hfToken.asStateFlow()

    fun setToken(token: String) {
        prefs.edit().putString("hf_token", token).apply()
        _hfToken.value = token
    }

    fun getAccessToken(): String? {
        return _hfToken.value
    }

    fun logout() {
        prefs.edit().clear().apply()
        _hfToken.value = null
    }

    fun isAuthorized(): Boolean {
        return !_hfToken.value.isNullOrBlank()
    }
}
