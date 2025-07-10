Starting from Android 9 (API level 28), the platform restricts the usage of non-SDK interfaces. These restrictions apply whenever an app references a non-SDK interface or tries to access it via reflection or JNI.  
These measures were introduced to enhance user and developer experience by reducing app crashes and minimizing emergency rollouts for developers.  
For more details, see Improving Stability by Reducing Usage of non-SDK Interfaces.

This library is **not** intended to replace existing libraries like [android-hidden-api](https://github.com/anggrayudi/android-hidden-api).  
It can be used immediately without any modifications to your existing code to access hidden APIs.

```kotlin
try { 
     val clazz = Class.forName("dalvik.system.VMRuntime")
     val method = clazz.getDeclaredMethod("setHiddenApiExemptions", Array<String>::class.java)
     Toast.makeText(context, "Invoke succeeded: $method", Toast.LENGTH_SHORT).show()
 } catch (e: Throwable) {
     Toast.makeText(context, "Invoke failed: ${e.message}", Toast.LENGTH_SHORT).show()
 }
```

The key advantage of this library is its minimal invasiveness — you don't need to change any existing code to support higher Android versions.
All you need to do is call:

```kotlin
HiddenApiCompat.compat(context)
```

when necessary, and the library will handle the rest seamlessly.