package gr.pkcoding.peoplescope.utils

object Constants {
    // API
    const val BASE_URL = "https://randomuser.me/"
    const val PAGE_SIZE = 25
    const val INITIAL_PAGE = 1

    // Database
    const val DATABASE_NAME = "peoplescope_database"
    const val DATABASE_VERSION = 1

    // Navigation
    const val NAV_USER_ID_KEY = "userId"

    // Network
    const val NETWORK_TIMEOUT = 30L
    const val API_TIMEOUT = 15_000L
    const val BOOKMARK_CACHE_TTL = 30_000L

    // Search
    const val SEARCH_DEBOUNCE_MS = 300L

    // Performance
    const val MEMORY_CACHE_PERCENT = 0.20
    const val DISK_CACHE_SIZE = 30L * 1024 * 1024
    const val OKHTTP = "OkHttp"
}