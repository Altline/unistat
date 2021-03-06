package domyutil

import java.util.prefs.Preferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Defines a new [PreferenceDelegate] tied to a preference of a specified [Preferences] instance with the specified key
 * string and a default value.
 */
inline fun <reified T : Any> preference(preferences: Preferences, key: String, defaultValue: T) =
    PreferenceDelegate(preferences, key, defaultValue, T::class)

/**
 * A delegated property for reading and writing data to a preference of the Java [Preferences] API.
 */
class PreferenceDelegate<T : Any>(
    private val preferences: Preferences,
    private val key: String,
    private val defaultValue: T,
    private val type: KClass<T>
) : ReadWriteProperty<Any, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return with(preferences) {
            when (type) {
                String::class -> get(key, defaultValue as String)
                Int::class -> getInt(key, defaultValue as Int)
                Long::class -> getLong(key, defaultValue as Long)
                Float::class -> getFloat(key, defaultValue as Float)
                Double::class -> getDouble(key, defaultValue as Double)
                Boolean::class -> getBoolean(key, defaultValue as Boolean)
                ByteArray::class -> getByteArray(key, defaultValue as ByteArray)
                else -> error("Unsupported preference type $type.")
            }
        } as T
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        with(preferences) {
            when (type) {
                String::class -> put(key, value as String)
                Int::class -> putInt(key, value as Int)
                Long::class -> putLong(key, value as Long)
                Float::class -> putFloat(key, value as Float)
                Double::class -> putDouble(key, value as Double)
                Boolean::class -> putBoolean(key, value as Boolean)
                ByteArray::class -> putByteArray(key, value as ByteArray)
                else -> error("Unsupported preference type $type.")
            }
        }
    }
}