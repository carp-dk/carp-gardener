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

        // Garmin properties
        const val GARMIN_CONSUMER_KEY = "garmin_key"
        const val GARMIN_CONSUMER_SECRET = "garmin_secret"
        const val GARMIN_REQUEST_TOKEN_URI = "https://connectapi.garmin.com/oauth-service/oauth/request_token"
        const val GARMIN_ACCESS_TOKEN_URI = "https://connectapi.garmin.com/oauth-service/oauth/access_token"
        const val GARMIN_AUTHORIZATION_URI = "https://connect.garmin.com/oauthConfirm"
        const val GARMIN_DATA_URI = "https://apis.garmin.com"
        const val GARMIN_TEST_USER_EXTERNAL_ID = "long_lived_token"

        // Withings properties
        const val WITHINGS_CLIENT_ID = "withings_id"
        const val WITHINGS_CLIENT_SECRET = "withings_secret"
        const val WITHINGS_AUTHORIZATION_URI = "https://account.withings.com/oauth2_user/authorize2"
        const val WITHINGS_TOKEN_URI = "https://wbsapi.withings.net/v2/oauth2"
        const val WITHINGS_DATA_URI = "https://wbsapi.withings.net"
        const val WITHINGS_TEST_USER_EXTERNAL_ID = "27444008"

        // Dexcom properties
        const val DEXCOM_CLIENT_ID = "dexcom_id"
        const val DEXCOM_CLIENT_SECRET = "dexcom_secret"
        const val DEXCOM_AUTHORIZATION_URI = "dexcom_authr_uri"
        const val DEXCOM_TOKEN_URI = "dexcom_token_uri"
        const val DEXCOM_DATA_URI = "dexcom_data_uri"
        const val DEXCOM_TEST_USER_EXTERNAL_ID = ""
        const val DEXCOM_PING_USER_ID = "userid"
    }
}
