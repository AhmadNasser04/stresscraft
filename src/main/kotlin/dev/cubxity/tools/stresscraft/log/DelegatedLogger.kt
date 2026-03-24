package dev.cubxity.tools.stresscraft.log

import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun logger() = DelegatedLogger

object DelegatedLogger {
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>): ReadOnlyProperty<Any, org.slf4j.Logger> {
        val logger = LoggerFactory.getLogger(thisRef.javaClass)
        return ReadOnlyProperty { _, _ -> logger }
    }
}