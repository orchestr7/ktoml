package com.akuleshov7.ktoml.utils

/**
 * @param T
 * @param iterator
 */
internal class LinesIteratorWrapper<T>(
    private val iterator: Iterator<T>
) : Iterator<T> {
    internal var lineNo: Int = 0
        private set

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): T {
        lineNo++
        return iterator.next()
    }
}
