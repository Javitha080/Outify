package cc.tomko.outify.core

import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class UserProfile @Inject constructor() {
    /**
     * Retrieves the users profile as a string
     */
    external fun getUserProfile(username: String? = null): String?
}
