package com.herocc.school.aspencheck

object Utils {
    @JvmStatic
    fun <K, V> Map<K, V>.subtract(other: Map<K, V>): Map<K, V> {
        return this.filter { entry ->
            if (!other.containsKey(entry.key)) return@filter true
            return@filter other[entry.key] == entry.value
        }
    }
}