package dk.carp.gardener.authentication.core.common.util.collections

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A List collection that only allows appending and
 * does not allow the modification of existing entries.
 */
class RestrictedList<T>(@JsonProperty("list") initList: MutableList<T>? = null) {


    private val list: MutableList<T>

    init {
        list = initList ?: mutableListOf()
    }

    fun add(element: T): Boolean {
        if (list.contains(element)) {
            return false
        }
        list.add(element)
        return true
    }

    fun addAll(elements: List<T>) {
        elements.forEach { element ->
            this.add(element)
        }
    }

    fun getList(): List<T> {
        return list
    }

}