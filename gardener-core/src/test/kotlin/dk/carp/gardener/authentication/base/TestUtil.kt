package dk.carp.gardener.authentication.base

import java.net.URL

/**
 * Utility class for test cases.
 */
class TestUtil private constructor() {
    companion object {
        /**
         * Reads up a resource from the resources folder and
         * returns the content of the resource as a String.
         */
        fun getResourceAsText(path: String): String {
            val resource: URL
            try {
                resource = object {}.javaClass.getResource(path)
            } catch (ex: Exception) {
                throw IllegalArgumentException("An error is encountered while trying to access resource:  $path: ${ex.message}")
            }
            return resource.readText()
        }
    }
}
