package common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {

    /**
     * Attempts to find a site-local (192.168.x.x, 10.x.x.x, etc.) IPv4 address.
     * If none is found, falls back to a non-loopback IPv4 address.
     * If all fails, returns null.
     */
    public static String getLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress candidateAddress = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Skip loopback or down interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // We only want IPv4
                    if (addr instanceof Inet4Address) {
                        if (addr.isSiteLocalAddress()) {
                            // Found a definitive LAN address (e.g. 192.168.x.x)
                            return addr.getHostAddress();
                        } else if (candidateAddress == null) {
                            // Found a non-loopback address (could be public), keep as backup
                            candidateAddress = addr;
                        }
                    }
                }
            }
            
            // Return candidate if found, otherwise null
            return (candidateAddress != null) ? candidateAddress.getHostAddress() : null;

        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }
}
