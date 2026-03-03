package com.example.islandlyrics

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import com.example.islandlyrics.service.UserService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object NetworkPolicyManager {
    private const val TAG = "NetworkPolicyManager"

    private var privilegedService: IPrivilegedService? = null
    private val connectionMutex = Mutex()

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name)
        )
        .daemon(false)
        .processNameSuffix("privileged")
        .version(2)
    }

    /**
     * Toggles Internet permission for a specific app package using Shizuku service.
     * @param packageName The target package name.
     * @param enable True to allow internet, false to block.
     */
    suspend fun toggleAppInternet(context: Context, packageName: String, enable: Boolean) {
        val uid = try {
            context.packageManager.getPackageUid(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            AppLogger.getInstance().e(TAG, "Package not found: $packageName")
            return
        }

        try {
            val service = getOrBindService()
            service.setPackageNetworkingEnabled(uid, enable)
            AppLogger.getInstance().log(TAG, "Toggled internet for $packageName (UID=$uid) to $enable")
        } catch (e: Exception) {
            AppLogger.getInstance().e(TAG, "Failed to toggle internet for $packageName", e)
            if (e is DeadObjectException) {
                // Invalidate cache and retry once
                privilegedService = null
                try {
                    val retryService = getOrBindService()
                    retryService.setPackageNetworkingEnabled(uid, enable)
                    AppLogger.getInstance().log(TAG, "Retry success: Toggled internet for $packageName to $enable")
                } catch (retryEx: Exception) {
                    AppLogger.getInstance().e(TAG, "Retry failed", retryEx)
                }
            }
        }
    }

    /**
     * Force stops a package using the privileged service.
     * @param packageName The name of the package to be force stopped.
     */
    suspend fun forceStopPackage(packageName: String) {
        try {
            val service = getOrBindService()
            service.forceStopPackage(packageName)
            AppLogger.getInstance().log(TAG, "Force stopped $packageName")
        } catch (e: Exception) {
            AppLogger.getInstance().e(TAG, "Failed to force stop $packageName", e)
            if (e is DeadObjectException) {
                privilegedService = null
                try {
                    val retryService = getOrBindService()
                    retryService.forceStopPackage(packageName)
                } catch (retryEx: Exception) {
                    AppLogger.getInstance().e(TAG, "Retry force stop failed", retryEx)
                }
            }
        }
    }

    /**
     * Ensures we have a valid AIDL interface, binding if necessary.
     */
    private suspend fun getOrBindService(): IPrivilegedService {
        return connectionMutex.withLock {
            val current = privilegedService
            if (current != null && current.asBinder().isBinderAlive) {
                return current
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Shizuku permission not granted")
            }

            return@withLock withTimeout(5000L) {
                suspendCancellableCoroutine<IPrivilegedService> { cont ->
                    val listener = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            AppLogger.getInstance().log(TAG, "onServiceConnected")
                            try {
                                val s = IPrivilegedService.Stub.asInterface(service)
                                privilegedService = s
                                if (cont.isActive) {
                                    cont.resume(s)
                                }
                            } catch (e: Exception) {
                                AppLogger.getInstance().e(TAG, "Failed to cast AIDL interface", e)
                                if (cont.isActive) cont.resumeWithException(e)
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            AppLogger.getInstance().log(TAG, "onServiceDisconnected")
                            privilegedService = null
                        }

                        override fun onBindingDied(name: ComponentName?) {
                            AppLogger.getInstance().log(TAG, "onBindingDied")
                            privilegedService = null
                            if (cont.isActive) {
                                cont.resumeWithException(DeadObjectException("Binding died during connection"))
                            }
                        }
                    }

                    try {
                        // Shizuku.bindUserService calls through to the manager
                        Shizuku.bindUserService(userServiceArgs, listener)
                        cont.invokeOnCancellation {
                            try {
                                Shizuku.unbindUserService(userServiceArgs, listener, true)
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        AppLogger.getInstance().e(TAG, "Bind call failed", e)
                        if (cont.isActive) {
                            cont.resumeWithException(e)
                        }
                    }
                }
            }
        }
    }
}
