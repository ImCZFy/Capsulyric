// IPrivilegedService.aidl
package com.example.islandlyrics;

// Declare the interface for the privileged service
interface IPrivilegedService {
    // Core function: Toggle network access for a specific UID directly via IConnectivityManager
    void setPackageNetworkingEnabled(int uid, boolean enabled);

    // Force stop a package to clear cache
    void forceStopPackage(String packageName);

    // Destroy the service process
    void destroy();
}
