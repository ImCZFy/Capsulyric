package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * Hidden API stub for IConnectivityManager.
 * Used to access internal firewall methods.
 */
public interface IConnectivityManager extends IInterface {

    // Stub definition for Binder
    abstract class Stub extends Binder implements IConnectivityManager {
        public static IConnectivityManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    /**
     * Set whether the firewall chain is enabled.
     * @param chain The chain to enable/disable.
     * @param enabled Whether the chain should be enabled.
     */
    void setFirewallChainEnabled(int chain, boolean enabled) throws RemoteException;

    /**
     * Set the firewall rule for a specific UID.
     * @param chain The chain to modify.
     * @param uid The UID to modify.
     * @param rule The rule to apply (e.g., ALLOW, DENY).
     */
    void setUidFirewallRule(int chain, int uid, int rule) throws RemoteException;
}

