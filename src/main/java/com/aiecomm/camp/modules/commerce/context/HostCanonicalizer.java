package com.aiecomm.camp.modules.commerce.context;

/**
 * Canonicalizes the raw HTTP Host header into a stable lookup key.
 * Strips www., lowercases, removes port number.
 *
 * Examples:
 *   "WWW.Example.COM:443" -> "example.com"
 *   "shop.KARWAN.pk"      -> "shop.karwan.pk"
 *   "localhost:3000"      -> "localhost"
 */
public final class HostCanonicalizer {

    private HostCanonicalizer() {}

    public static String canonicalize(String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            return "";
        }
        String host = rawHost.toLowerCase().trim();
        // Remove port
        int portIdx = host.lastIndexOf(':');
        if (portIdx > 0 && portIdx < host.length() - 1) {
            String potentialPort = host.substring(portIdx + 1);
            if (potentialPort.chars().allMatch(Character::isDigit)) {
                host = host.substring(0, portIdx);
            }
        }
        // Strip www.
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return host;
    }
}
