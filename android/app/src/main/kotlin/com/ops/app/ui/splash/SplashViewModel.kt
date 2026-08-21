package com.ops.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.datastore.AuthPreferences
import com.ops.app.data.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Home : SplashDestination
    data object BusinessSetup : SplashDestination
}

/** "Splash → routes to Business Setup (no local business yet) or Home" per
 * DISCOVERY.md section 10. Both signals (signed in AND a local business row)
 * are checked so a logout — which clears both DataStore and Room — reliably
 * routes back to Business Setup rather than a half-signed-out limbo state. */
@HiltViewModel
class SplashViewModel @Inject constructor(
    authPreferences: AuthPreferences,
    businessRepository: BusinessRepository,
) : ViewModel() {

    val destination: StateFlow<SplashDestination> = combine(
        authPreferences.isSignedIn,
        businessRepository.observe(),
    ) { signedIn, business ->
        if (signedIn && business != null) SplashDestination.Home else SplashDestination.BusinessSetup
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SplashDestination.Loading)
}
