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
            val resource: URL =
                object {}.javaClass.getResource(path)
                    ?: throw IllegalArgumentException("Resource not found at path: $path")
            return resource.readText()
        }
    }
}
