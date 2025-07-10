# Android Hidden API Compatibility Library

Starting from Android 9 (API level 28), the platform enforces restrictions on non-SDK interface usage. These restrictions apply when apps attempt to access non-SDK interfaces through reflection or JNI.  
These measures were implemented to enhance stability by reducing unexpected crashes and minimizing emergency updates for developers.  
For more details, see Google's official documentation: [Improving Stability by Reducing Usage of non-SDK Interfaces](https://developer.android.com/about/versions/pie/restrictions-non-sdk-interfaces).

```kotlin
try { 
    val clazz = Class.forName("dalvik.system.VMRuntime")
    val method = clazz.getDeclaredMethod("setHiddenApiExemptions", Array<String>::class.java)
    Toast.makeText(context, "API access succeeded: $method", Toast.LENGTH_SHORT).show()
} catch (e: Throwable) {
    Toast.makeText(context, "API access failed: ${e.message}", Toast.LENGTH_SHORT).show()
}
```

## Key Features
- **Zero Code Modification** - Works with existing codebase without any changes
- **Automatic Exemption Handling** - Transparently manages hidden API access restrictions
- **Lightweight Integration** - Single initialization call for seamless operation
- **Cross-Version Support** - Compatible with Android 9+ (API 28 and higher)

## Installation

Add dependency in your app's build.gradle:

```groovy
dependencies {
    implementation 'io.github.chanjlee:hiddenapi:1.0.0'
}
```

## Usage

Initialize once in your Application class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HiddenApiCompat.compat(this)
    }
}
```

## How It Works

The library implements the following mechanisms:

- Automatic Signature Bypass - Utilizes Android's hidden API exemption protocol
- Reflection Optimization - Smart handling of reflection requirements
- Runtime Safety - Maintains original app behavior while enabling privileged access

## Compatibility Matrix

| Android Version | API Level | Support Status | 
|-----------------|-----------|-----------------| 
| Android 9 | 28 | ✔️ Full | | Android 10 | 29 | ✔️ Full | | Android 11+ | 30+ | ✔️ Full |
