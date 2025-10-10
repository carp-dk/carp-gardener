package dk.carp.gardener.authentication.core.common.util.collections

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A Map collection that only allows appending and
 * does not allow the modification of existing entries.
 */
class RestrictedMap<K, V>(@JsonProperty("map") initMap: MutableMap<K, V>? = null) {

    private val map: MutableMap<K, V>

    init {
        map = initMap ?: mutableMapOf()
    }

    fun put(key: K, value: V): Boolean {
        if (map.containsKey(key)) {
            return false
        }
        map[key] = value
        return true
    }

    fun getMap(): Map<K, V> {
        return map
    }

}