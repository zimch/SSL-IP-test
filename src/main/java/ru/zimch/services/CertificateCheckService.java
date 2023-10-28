package ru.zimch.services;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zimch.utils.DomainUtil;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class CertificateCheckService {

    final static Logger logger = LoggerFactory.getLogger(CertificateCheckService.class);

    public static void findDomains(String ipAddress, int threadCount) {

        List<Thread> threads = new ArrayList<>();

        try {
            List<String> ipAddresses = getIPRange(ipAddress);

            int addressesPerThread = ipAddresses.size() / threadCount;

            for (int i = 0; i < threadCount; i++) {
                int startIndex = i * addressesPerThread;
                int endIndex = startIndex + addressesPerThread;

                if (i == threadCount - 1) {
                    endIndex = ipAddresses.size();
                }

                final List<String> addressesForThread = ipAddresses.subList(startIndex, endIndex);

                Thread thread = new Thread(() -> {
                    scanAddresses(addressesForThread);
                });

                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        } catch (UnknownHostException e) {
            logger.error("[UnknownHostException]: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("[InterruptedException]: " + e.getMessage());
        }
    }

    private static List<String> getIPRange(String ipAddress) throws UnknownHostException {
        int subnetMaskBits = Integer.parseInt(ipAddress.substring(ipAddress.indexOf("/") + 1));
        String subnetAddress = ipAddress.substring(0, ipAddress.indexOf("/"));

        int shift = 32 - subnetMaskBits;
        int numberOfAddresses = (int) Math.pow(2, shift);

        byte[] startAddress = InetAddress.getByName(subnetAddress).getAddress();
        int startIP = (startAddress[0] & 0xFF) << 24 | (startAddress[1] & 0xFF) << 16 | (startAddress[2] & 0xFF) << 8 | (startAddress[3] & 0xFF);

        List<String> ipAddresses = new ArrayList<>();

        for (int i = 0; i < numberOfAddresses; i++) {
            int ipInInt = startIP + i;
            String ip = InetAddress.getByAddress(new byte[]{(byte) (ipInInt >> 24), (byte) (ipInInt >> 16), (byte) (ipInInt >> 8), (byte) ipInInt}).getHostAddress();
            ipAddresses.add(ip);
        }

        return ipAddresses;
    }

    private static void scanAddresses(List<String> addresses) {

        String PEER_CERTIFICATES = "PEER_CERTIFICATES";

        for (String address : addresses) {

            HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
                ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
                SSLSession sslSession = routedConnection.getSSLSession();
                if (sslSession != null) {

                    Certificate[] certificates = sslSession.getPeerCertificates();

                    context.setAttribute(PEER_CERTIFICATES, certificates);
                }
            };

            CloseableHttpClient httpClient = HttpClients
                    .custom()
                    .addInterceptorLast(certificateInterceptor)
                    .build();

            try {
                InetAddress inetAddress = InetAddress.getByName(address);
                if (inetAddress.isReachable(200)) {
                    try {
                        HttpGet httpget = new HttpGet("https://" + inetAddress.getHostName());
                        logger.debug("[Executing request]: " + httpget.getRequestLine());

                        HttpContext context = new BasicHttpContext();
                        httpClient.execute(httpget, context);

                        Certificate[] peerCertificates = (Certificate[])context.getAttribute(PEER_CERTIFICATES);

                        X509Certificate real = (X509Certificate) peerCertificates[0];
                        DomainUtil.writeDomainToFile(real.getSubjectX500Principal().getName());
                    } catch (IOException e) {
                        logger.error("[IOException]: " + e.getLocalizedMessage());
                    }
                }


            } catch (UnknownHostException e) {
                logger.error("[UnknownHostException]: " + e.getMessage());
            } catch (IOException e) {
                logger.error("[IOException]: " + e.getMessage());
            }
        }
    }
}
