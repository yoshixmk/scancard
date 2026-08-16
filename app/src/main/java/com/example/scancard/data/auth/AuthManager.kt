package com.example.scancard.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.openid.appauth.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val authService = AuthorizationService(context)
    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://huggingface.co/oauth/authorize"),
        Uri.parse("https://huggingface.co/oauth/token")
    )

    // Configuration - should be moved to a safe place
    private val clientId = "YOUR_HF_CLIENT_ID" 
    private val redirectUri = Uri.parse("com.example.scancard.auth://oauth2redirect")

    private val _authState = MutableStateFlow<AuthState?>(loadAuthState())
    val authState = _authState.asStateFlow()

    fun getAuthIntent(): Intent {
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScope("openid profile read-repos").build()

        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleAuthResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response != null) {
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                val state = _authState.value ?: AuthState(serviceConfig)
                state.update(tokenResponse, tokenException)
                _authState.value = state
                saveAuthState(state)
            }
        }
    }

    fun getAccessToken(): String? {
        return _authState.value?.accessToken
    }

    private fun saveAuthState(state: AuthState) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().putString("state", state.jsonSerializeString()).apply()
    }

    private fun loadAuthState(): AuthState? {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val json = prefs.getString("state", null) ?: return null
        return AuthState.jsonDeserialize(json)
    }

    fun logout() {
        _authState.value = null
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
