package cc.tomko.outify.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor() {
    external fun search()
}