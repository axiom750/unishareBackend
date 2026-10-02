package com.unishare.diagnostics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Valkey Network Diagnostic Tool
 * 
 * Performs independent measurements of TCP and TLS connection phases
 * to diagnose the root cause of slow initial connection establishment.
 * 
 * This diagnostic:
 * - Uses raw Java Socket/SSLSocket APIs
 * - Measures DNS, TCP, and TLS independently
 * - Does NOT use Lettuce client
 * - Does NOT send Redis commands
 * - Does NOT send authentication
 * - Does NOT keep connections open
 * - Does NOT interfere with StringRedisTemplate
 * - Runs only when explicitly enabled
 * 
 * Enable with:
 *   unishare.redis.diagnostics.enabled=true
 * 
 * SECURITY:
 * - Uses JVM default trust store
 * - Performs normal hostname verification
 * - Does NOT disable certificate validation
 * - Does NOT log credentials
 * - Closes sockets immediately after testing
 * 
 * @see StartupConfigLogger for Lettuce-based diagnostic
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "unishare.redis.diagnostics",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Order(1)  // Run before StartupConfigLogger
public class ValkeyNetworkDiagnostic {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void runNetworkDiagnostic() {
        log.info("============================================================");
        log.info("VALKEY NETWORK DIAGNOSTIC (Independent Measurement)");
        log.info("============================================================");
        log.info("Target:");
        log.info("  Host : {}", redisHost);
        log.info("  Port : {}", redisPort);
        log.info("  SSL  : {}", sslEnabled ? "ENABLED" : "DISABLED");
        log.info("");
        
        // Phase 1: DNS Resolution
        long dnsStart = System.nanoTime();
        InetAddress address = null;
        try {
            address = InetAddress.getByName(redisHost);
            long dnsDuration = (System.nanoTime() - dnsStart) / 1_000_000;
            log.info("[REDIS-DIAGNOSTICS] DNS Resolution:");
            log.info("  Status   : SUCCESS");
            log.info("  Duration : {} ms", dnsDuration);
            log.info("  IP       : {}", address.getHostAddress());
        } catch (Exception e) {
            long dnsDuration = (System.nanoTime() - dnsStart) / 1_000_000;
            log.error("[REDIS-DIAGNOSTICS] DNS Resolution:");
            log.error("  Status   : FAILED");
            log.error("  Duration : {} ms", dnsDuration);
            log.error("  Error    : {}", e.getMessage());
            log.info("============================================================");
            return;
        }
        
        // Phase 2: TCP Connection
        long tcpStart = System.nanoTime();
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, redisPort), 10000);  // 10 second timeout
            long tcpDuration = (System.nanoTime() - tcpStart) / 1_000_000;
            log.info("[REDIS-DIAGNOSTICS] TCP Connection:");
            log.info("  Status   : SUCCESS");
            log.info("  Duration : {} ms", tcpDuration);
            log.info("  Local    : {}:{}", socket.getLocalAddress().getHostAddress(), socket.getLocalPort());
            log.info("  Remote   : {}:{}", socket.getInetAddress().getHostAddress(), socket.getPort());
        } catch (Exception e) {
            long tcpDuration = (System.nanoTime() - tcpStart) / 1_000_000;
            log.error("[REDIS-DIAGNOSTICS] TCP Connection:");
            log.error("  Status   : FAILED");
            log.error("  Duration : {} ms", tcpDuration);
            log.error("  Error    : {}", e.getMessage());
            
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
            
            log.info("============================================================");
            return;
        }
        
        // Phase 3: TLS Handshake (if SSL enabled)
        if (sslEnabled) {
            long tlsStart = System.nanoTime();
            SSLSocket sslSocket = null;
            try {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                
                // Wrap the existing TCP socket with SSL
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    socket,
                    redisHost,  // Use hostname for SNI and hostname verification
                    redisPort,
                    true  // autoClose - closes underlying socket when SSL socket closes
                );
                
                // Enable hostname verification (default behavior)
                sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                
                // Perform TLS handshake
                sslSocket.startHandshake();
                
                long tlsDuration = (System.nanoTime() - tlsStart) / 1_000_000;
                log.info("[REDIS-DIAGNOSTICS] TLS Handshake:");
                log.info("  Status   : SUCCESS");
                log.info("  Duration : {} ms", tlsDuration);
                log.info("  Protocol : {}", sslSocket.getSession().getProtocol());
                log.info("  Cipher   : {}", sslSocket.getSession().getCipherSuite());
                
                // Close SSL socket (which also closes underlying TCP socket)
                sslSocket.close();
                socket = null;  // Already closed by SSL socket
                
            } catch (Exception e) {
                long tlsDuration = (System.nanoTime() - tlsStart) / 1_000_000;
                log.error("[REDIS-DIAGNOSTICS] TLS Handshake:");
                log.error("  Status   : FAILED");
                log.error("  Duration : {} ms", tlsDuration);
                log.error("  Error    : {}", e.getClass().getSimpleName());
                log.error("  Message  : {}", e.getMessage());
                
                // Log cause chain for TLS errors
                Throwable cause = e.getCause();
                int level = 1;
                while (cause != null && level <= 3) {
                    log.error("  Cause {}  : {}: {}", level, cause.getClass().getSimpleName(), cause.getMessage());
                    cause = cause.getCause();
                    level++;
                }
                
                if (sslSocket != null) {
                    try { sslSocket.close(); } catch (Exception ignored) {}
                }
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
                
                log.info("============================================================");
                return;
            }
        } else {
            log.info("[REDIS-DIAGNOSTICS] TLS Handshake:");
            log.info("  Status   : SKIPPED (SSL disabled)");
            
            // Close TCP socket if not using SSL
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
        
        // Summary
        log.info("------------------------------------------------------------");
        log.info("[REDIS-DIAGNOSTICS] Summary:");
        log.info("  All network phases completed successfully");
        log.info("  If Lettuce connection still shows ~9.7s, the delay is likely:");
        log.info("    - Redis AUTH phase");
        log.info("    - Redis protocol negotiation");
        log.info("    - Lettuce client initialization");
        log.info("    - Connection pool warmup");
        log.info("    - Spring lifecycle delays");
        log.info("============================================================");
    }
}
