package cc.tomko.outify.ui.viewmodel.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.AuthCallbackServerManager
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.AuthStateEventBus
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.UserProfile
import cc.tomko.outify.core.model.CurrentUserProfile
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.metadata.NativeErrorHandler
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.services.OAuthService
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    val spClient: SpClient,
    val userProfile: UserProfile,
    val authManager: AuthManager,
    private val spircController: SpircController,
    private val settingsRepository: SettingsRepository,
    private val serverManager: AuthCallbackServerManager,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _isPlaybackLoggedIn = MutableStateFlow(false)
    val isPlaybackLoggedIn: StateFlow<Boolean> = _isPlaybackLoggedIn.asStateFlow()

    private val _isAccountLoggedIn = MutableStateFlow(false)
    val isAccountLoggedIn: StateFlow<Boolean> = _isAccountLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userImageUrl = MutableStateFlow<String?>(null)
    val userImageUrl: StateFlow<String?> = _userImageUrl.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _scopes = MutableStateFlow<List<String>>(emptyList())
    val scopes: StateFlow<List<String>> = _scopes.asStateFlow()

    init {
        checkAuthState()
        loadSavedUserProfile()
    }

    override fun onCleared() {
        super.onCleared()
        serverManager.stop()
    }

    fun checkAuthState() {
        _isAccountLoggedIn.value = runCatching { spClient.isOAuthAuthenticated() }.getOrDefault(false)
        _isPlaybackLoggedIn.value = runCatching { authManager.hasCachedCredentials() }.getOrDefault(false)

        _scopes.value = runCatching { spClient.getOAuthScope()?.split(" ") }.getOrNull() ?: emptyList()
    }

    private fun loadSavedUserProfile() {
        viewModelScope.launch {
            _username.value = settingsRepository.username.first()
            _userImageUrl.value = settingsRepository.userImageUrl.first()
        }
    }

    fun startSpircAuth(context: Context) {
        OAuthService.start(context)

        serverManager.start(onCodeReceived = { code, state ->
            OAuthService.stop(context)
            val result = runCatching { authManager.handleOAuthCode(code, state) }.getOrElse {
                android.util.Log.w("AccountsViewModel", "Spirc auth failed", it)
                "{\"error\":{\"type\":\"unknown\",\"message\":\"Auth failed\"}}"
            }
            val isSuccess = result.contains("\"success\":true")
            val errorDetails = if (!isSuccess) parseErrorMessage(result) else null
            if (!isSuccess) {
                NativeErrorHandler.handleErrorJson(result, "spirc oauth")
            }
            GlobalPopupController.show(PopupSpec.AuthResult(isSuccess, errorDetails = errorDetails))
            if (isSuccess) {
                _isPlaybackLoggedIn.value = runCatching { authManager.hasCachedCredentials() }.getOrDefault(true)
                checkAuthState()
                AuthStateEventBus.tryEmitPlaybackLoggedIn()
                spircController.restart()
            }
        })

        val url = runCatching { authManager.getAuthorizationURL() }.getOrNull() ?: return

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            android.util.Log.w("AccountsViewModel", "Browser launch failed", it)
            serverManager.stop()
            OAuthService.stop(context)
        }
    }

    fun startAccountAuth(context: Context) {
        OAuthService.start(context)

        serverManager.start(onCodeReceived = { code, state ->
            OAuthService.stop(context)
            val result = runCatching { spClient.completeOAuthFlow(code) }.getOrElse {
                android.util.Log.w("AccountsViewModel", "Account auth failed", it)
                "{\"error\":{\"type\":\"unknown\",\"message\":\"Auth failed\"}}"
            }
            val isSuccess = result.contains("\"success\":true")
            val errorDetails = if (!isSuccess) parseErrorMessage(result) else null
            if (!isSuccess) {
                NativeErrorHandler.handleErrorJson(result, "account oauth")
            }
            GlobalPopupController.show(PopupSpec.AuthResult(isSuccess, errorDetails = errorDetails))
            if (isSuccess) {
                viewModelScope.launch {
                    delay(100)
                    var authenticated = runCatching { spClient.isOAuthAuthenticated() }.getOrDefault(false)
                    if (!authenticated) {
                        delay(300)
                        authenticated = runCatching { spClient.isOAuthAuthenticated() }.getOrDefault(false)
                    }
                    _isAccountLoggedIn.value = authenticated
                    checkAuthState()
                    AuthStateEventBus.tryEmitAccountLoggedIn()
                    if (authenticated) {
                        fetchProfile()
                    }
                }
            }
        })

        val url = runCatching { spClient.startOAuthFlow() }.getOrNull() ?: run {
            serverManager.stop()
            OAuthService.stop(context)
            return
        }

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            android.util.Log.w("AccountsViewModel", "Browser launch failed", it)
            serverManager.stop()
            OAuthService.stop(context)
        }
    }

    fun logoutPlayback() {
        authManager.logout()
        _isPlaybackLoggedIn.value = false
    }

    fun logoutAccount() {
        runCatching { spClient.logout() }
        _isAccountLoggedIn.value = false

        viewModelScope.launch {
            try {
                settingsRepository.removeUserProfile()
                _scopes.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.w("AccountsViewModel", "Profile clear failed", e)
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val profile = spClient.getCurrentUserProfile()
                if (profile == null) {
                    return@launch
                }
                spClient.checkAndHandleError(profile, "fetchProfile")
                val jsonObject = json.decodeFromString<CurrentUserProfile>(profile)

                val id = jsonObject.id
                val username = jsonObject.displayName
                val imageUrl = jsonObject.images.firstOrNull()?.url

                _isPremium.value = jsonObject.product == "premium"

                _username.value = username
                _userImageUrl.value = imageUrl

                settingsRepository.saveUserProfile(id, username, imageUrl)
            } catch (e: Exception) {
                android.util.Log.w("AccountsViewModel", "Profile fetch failed")
            }
        }
    }

    private fun parseErrorMessage(json: String): String? {
        return try {
            val obj = org.json.JSONObject(json)
            if (obj.has("error")) {
                val err = obj.getJSONObject("error")
                err.optString("message", null)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}