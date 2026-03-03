package com.example.islandlyrics.service


import android.net.IConnectivityManager
import android.os.IBinder
import android.os.RemoteException
import com.example.islandlyrics.IPrivilegedService
import com.example.islandlyrics.AppLogger
import kotlin.system.exitProcess

open class PrivilegedServiceImpl : IPrivilegedService.Stub() {

    private val iConnectivityManager: IConnectivityManager?

    companion object {
        const val TAG = "PrivilegedServiceImpl"
    }

    init {
        var cm: IConnectivityManager? = null
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "connectivity") as IBinder
            cm = IConnectivityManager.Stub.asInterface(binder)
            AppLogger.getInstance().log(TAG, "ConnectivityManager binder acquired")
        } catch (e: Exception) {
            AppLogger.getInstance().e(TAG, "Failed to acquire ConnectivityManager binder", e)
        }
        iConnectivityManager = cm
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean) {
        if (iConnectivityManager == null) {
            AppLogger.getInstance().e(TAG, "IConnectivityManager is null, cannot toggle network")
            return
        }

        try {
            val cm = iConnectivityManager

            // The integer 3 actually means FIREWALL_CHAIN_POWERSAVE (Whitelist mode).
            // use 9, which represents FIREWALL_CHAIN_OEM_DENY_3 (Blacklist mode).
            val chain = 9

            // FIREWALL_RULE_DEFAULT = 0, FIREWALL_RULE_ALLOW = 1, FIREWALL_RULE_DENY = 2
            // For a DENY chain, use DENY (2) to block, and DEFAULT (0) to remove the block.
            val rule = if (enabled) 0 else 2

            if (!enabled) {
                // Block network: Ensure the chain is enabled, then apply DENY rule to the UID
                cm.setFirewallChainEnabled(chain, true)
                cm.setUidFirewallRule(chain, uid, rule)
                AppLogger.getInstance().log(TAG, "Network BLOCKED for UID: $uid via OEM_DENY_3")
            } else {
                // Restore network: Reset the UID rule to DEFAULT to remove the restriction
                cm.setUidFirewallRule(chain, uid, rule)
                // WARNING: Do NOT disable the entire chain here, otherwise other apps blocked
                // in this chain will also regain network access unexpectedly.
                AppLogger.getInstance().log(TAG, "Network RESTORED for UID: $uid via OEM_DENY_3")
            }
        } catch (e: Exception) {
            AppLogger.getInstance().e(TAG, "Failed to set package networking via AIDL Stub", e)
            throw RemoteException("AIDL Stub invocation failed: ${e.message}")
        }
    }

    override fun forceStopPackage(packageName: String) {
        try {
            // Use ActivityManager hidden API or simpler shell command since we are in shell/root context
            // "am force-stop" is reliable
            val command = arrayOf("am", "force-stop", packageName)
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            AppLogger.getInstance().log(TAG, "Force stopped package: $packageName")
        } catch (e: Exception) {
            AppLogger.getInstance().e(TAG, "Failed to force stop package: $packageName", e)
        }
    }

    override fun destroy() {
        AppLogger.getInstance().log(TAG, "Destroying PrivilegedServiceImpl")
        exitProcess(0)
    }
}
