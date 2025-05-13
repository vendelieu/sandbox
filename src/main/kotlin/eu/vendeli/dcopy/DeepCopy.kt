package eu.vendeli.dcopy

import java.beans.Introspector
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

@Suppress("UNCHECKED_CAST")
fun <T> Any?.deepCopy(): T {
    return deepCopyInternal(this, IdentityHashMap()) as T
}

@Suppress("UNCHECKED_CAST")
private fun deepCopyInternal(original: Any?, visited: IdentityHashMap<Any, Any>): Any? {
    if (original == null) return null
    when (original) {
        is String, is Number, is Boolean, is Char -> return original
        is Enum<*> -> return original
    }

    val clazz = original.javaClass

    if (Proxy.isProxyClass(clazz)) {
        val handler = Proxy.getInvocationHandler(original)
        return Proxy.newProxyInstance(
            clazz.classLoader,
            clazz.interfaces,
            handler
        )
    }

    visited[original]?.let { return it }

    if (clazz.isArray) {
        val length = java.lang.reflect.Array.getLength(original)
        val compType = clazz.componentType
        val newArray = java.lang.reflect.Array.newInstance(compType, length)
        visited[original] = newArray
        for (i in 0 until length) {
            val value = java.lang.reflect.Array.get(original, i)
            val copiedValue = deepCopyInternal(value, visited)
            java.lang.reflect.Array.set(newArray, i, copiedValue)
        }
        return newArray
    }

    if (original is Collection<*>) {
        val copyCollection: MutableCollection<Any?> = try {
            clazz.getDeclaredConstructor().newInstance() as MutableCollection<Any?>
        } catch (_: Exception) {
            when (original) {
                is List<*> -> ArrayList()
                is Set<*> -> LinkedHashSet()
                else -> ArrayList()
            }
        }
        visited[original] = copyCollection
        original.forEach { item ->
            copyCollection.add(deepCopyInternal(item, visited))
        }
        return copyCollection
    }

    if (original is Map<*, *>) {
        val copyMap: MutableMap<Any?, Any?> = try {
            clazz.getDeclaredConstructor().newInstance() as MutableMap<Any?, Any?>
        } catch (_: Exception) {
            LinkedHashMap()
        }
        visited[original] = copyMap
        original.forEach { (key, value) ->
            val keyCopy = deepCopyInternal(key, visited)
            val valueCopy = deepCopyInternal(value, visited)
            copyMap[keyCopy] = valueCopy
        }
        return copyMap
    }

    val newInstance = try {
        instantiateUsingBestConstructor(clazz, original, visited)
    } catch (e: Exception) {
        throw IllegalArgumentException("Cannot instantiate ${clazz.name}", e)
    }

    visited[original] = newInstance
    copyRemainingProperties(original, newInstance, visited)
    return newInstance
}

private fun instantiateUsingBestConstructor(
    clazz: Class<*>,
    original: Any,
    visited: IdentityHashMap<Any, Any>
): Any {
    // 1) Build property map from original
    val propMap = buildPropertyMap(original)
    // 2) Try multi-arg constructors
    return try {
        instantiateWithArgs(clazz, propMap, visited)
    } catch (_: NoSuchMethodException) {
        // 3) Fallback to no-arg + setters
        instantiateNoArgThenSet(clazz, propMap, visited)
    }
}

private fun copyRemainingProperties(
    original: Any,
    copy: Any,
    visited: IdentityHashMap<Any, Any>
) {
    val beanInfo = Introspector.getBeanInfo(original.javaClass, Any::class.java)
    beanInfo.propertyDescriptors.forEach { prop ->
        if (prop.name == "class") return@forEach
        val readMethod = prop.readMethod
        val writeMethod = prop.writeMethod
        if (readMethod != null && writeMethod != null) {
            try {
                if (!isAlreadySet(copy, readMethod)) {
                    val value = readMethod.invoke(original)
                    val copiedValue = deepCopyInternal(value, visited)
                    writeMethod.invoke(copy, copiedValue)
                }
            } catch (_: Exception) {
                // Ignore properties that can't be copied
            }
        }
    }
}

fun buildPropertyMap(original: Any): Map<String, Any?> {
    val props = Introspector
        .getBeanInfo(original.javaClass, Any::class.java)
        .propertyDescriptors
        .filter { it.readMethod != null }
    return props.associate { pd ->
        pd.name to pd.readMethod.invoke(original)
    }
}

fun <T : Any> instantiateWithArgs(
    clazz: Class<T>,
    propMap: Map<String, Any?>,
    visited: IdentityHashMap<Any, Any>
): T {
    // Sort constructors by descending parameter count
    val ctors = clazz.declaredConstructors
        .sortedByDescending { it.parameterCount }
    for (ctor in ctors) {
        val params = ctor.parameters
        // Try to resolve each parameter by name from propMap
        val args = arrayOfNulls<Any>(params.size)
        var ok = true
        for ((i, p) in params.withIndex()) {
            // Parameter names available if compiled with -parameters or by Kotlin compiler
            val name = p.name
            if (!propMap.containsKey(name)) {
                ok = false; break
            }
            args[i] = deepCopyInternal(propMap[name], visited)
        }
        if (!ok) continue
        ctor.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return ctor.newInstance(*args) as T    // Instantiate
    }
    throw NoSuchMethodException("No matching constructor for ${clazz.name}")
}

fun <T : Any> instantiateNoArgThenSet(
    clazz: Class<T>,
    propMap: Map<String, Any?>,
    visited: IdentityHashMap<Any, Any>
): T {
    val instance = clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
    val beanInfo = Introspector.getBeanInfo(clazz, Any::class.java)
    for (pd in beanInfo.propertyDescriptors) {
        val write = pd.writeMethod ?: continue
        val name = pd.name
        if (propMap.containsKey(name)) {
            val value = deepCopyInternal(propMap[name], visited)
            write.invoke(instance, value)
        }
    }
    return instance
}

private fun isAlreadySet(
    copy: Any,
    readMethod: Method
): Boolean {
    return try {
        // Check if property was already set via constructor
        readMethod.invoke(copy) != null
    } catch (_: Exception) {
        false
    }
}