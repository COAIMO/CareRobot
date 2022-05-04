package com.jeongmin.nurimotortester.Nuri

inline fun <reified E : Enum<E>, K> EnumCodesMap(crossinline getKey: (E) -> K) = object : EnumCodesMap<E, K> {
    override val codesMap = enumValues<E>().associate { getKey(it) to it }
}

interface EnumCodesMap<E : Enum<E>, K> {
    val codesMap: Map<K, E>
}