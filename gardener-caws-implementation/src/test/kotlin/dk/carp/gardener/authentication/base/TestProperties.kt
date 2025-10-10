package dk.carp.gardener.authentication.base

/**
 * Utility class that holds test properties.
 */
class TestProperties private constructor() {

    companion object {
        // Fitbit properties
        const val FITBIT_CLIENT_ID = "fitbit_id"
        const val FITBIT_CLIENT_SECRET = "fitbit_secret"
        const val FITBIT_AUTHORIZATION_URI = "https://www.fitbit.com/oauth2/authorize"
        const val FITBIT_TOKEN_URI = "https://api.fitbit.com/oauth2/token"
        const val FITBIT_DATA_URI = "https://api.fitbit.com"
        const val FITBIT_TEST_USER_EXTERNAL_ID = "FITBIT_EXTERNAL_ID"
    }

}