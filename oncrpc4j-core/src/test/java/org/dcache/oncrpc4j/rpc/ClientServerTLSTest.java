package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrString;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ClientServerTLSTest {

    private static final int PROGNUM = 100017;
    private static final int PROGVER = 1;

    private static final int NULL = 0;
    private static final int ECHO = 1;
    private static final int UPPER = 2;

    private OncRpcSvc svc;
    private OncRpcSvc clnt;
    private RpcCall clntCall;
    private SSLContext sslServerContext;
    private SSLContext sslClientContext;

    private RpcDispatchable echo = (RpcCall call) -> {
        switch (call.getProcedure()) {
            case ECHO: {
                XdrString s = new XdrString();
                call.retrieveCall(s);
                call.reply(s);
                break;
            }
            case UPPER: {
                RpcCall cb = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), call.getTransport());
                XdrString s = new XdrString();
                call.retrieveCall(s);
                cb.call(ECHO, s, s);
                call.reply(s);
                break;
            }
            case NULL: {
                call.reply(XdrVoid.XDR_VOID);
            }
        }
    };

    private RpcDispatchable upper = (RpcCall call) -> {
        XdrString s = new XdrString();
        call.retrieveCall(s);
        XdrString u = new XdrString(s.stringValue().toUpperCase());
        call.reply(u);
    };


    @BeforeClass
    public static void setupClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setUp() throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Certificate certificate = generateSelfSignedCert(keyPair);

        sslServerContext = createServerSslContext(certificate, keyPair.getPrivate());
        sslClientContext = createClientSslContext(certificate);
    }

    @After
    public void tearDown() throws IOException {
        if (svc != null) {
            svc.stop();
        }
        if (clnt != null) {
            clnt.stop();
        }
    }

    @Test
    public void shouldCallCorrectProcedure() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withSSLContext(sslClientContext)
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);


        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();
        clntCall.call(ECHO, s, reply);

        assertEquals("reply mismatch", s, reply);
    }


    @Test
    public void shouldTriggerClientCallback() throws IOException {

                svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withSSLContext(sslClientContext)
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);


        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();

        clntCall.call(UPPER, s, reply);

        assertEquals("reply mismatch", s.stringValue().toUpperCase(), reply.stringValue());
    }

    @Test
    public void shouldStartTLSHandshake() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withStartTLS()
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withSSLContext(sslClientContext)
                .withStartTLS()
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();

        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();

        clntCall.call(ECHO, s, reply);

        assertEquals("reply mismatch", s, reply);
    }

    @Test
    public void shouldStartDTLSHandshake() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withUDP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withStartTLS()
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withUDP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withSSLContext(sslClientContext)
                .withStartTLS()
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.UDP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();

        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();

        clntCall.call(ECHO, s, reply);

        assertEquals("reply mismatch", s, reply);
    }

    @Test
    public void shouldIdentifyTLSConnection() throws IOException {

        svc = new OncRpcSvcBuilder()
              .withoutAutoPublish()
              .withTCP()
              .withWorkerThreadIoStrategy()
              .withBindAddress("127.0.0.1")
              .withSelectorThreadPoolSize(1)
              .withWorkerThreadPoolSize(1)
              .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
              .withSSLContext(sslServerContext)
              .withStartTLS()
              .withServiceName("svc")
              .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
              .withoutAutoPublish()
              .withTCP()
              .withClientMode()
              .withWorkerThreadIoStrategy()
              .withSelectorThreadPoolSize(1)
              .withWorkerThreadPoolSize(1)
              .withSSLContext(sslClientContext)
              .withStartTLS()
              .withServiceName("clnt")
              .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);

        assertFalse("TLS not expected", t.isTLS());
        clntCall.startTLS();
        assertTrue("Expected TLS", t.isTLS());
    }

    @Test(expected = RpcAuthException.class)
    public void shouldFailWhenNoTLSOnClient() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withStartTLS()
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();
    }

    @Test(expected = Exception.class)
    public void shouldFailSecondStartTLS() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withStartTLS()
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withSSLContext(sslClientContext)
                .withStartTLS()
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();
        // REVISIT: the server throws EOF and prevents us from testing the local initialization
        clntCall.startTLS();
    }

    @Test
    public void shouldAcceptSecondConnectionInPlain() throws IOException {

        svc = new OncRpcSvcBuilder()
              .withoutAutoPublish()
              .withTCP()
              .withWorkerThreadIoStrategy()
              .withBindAddress("127.0.0.1")
              .withSelectorThreadPoolSize(1)
              .withWorkerThreadPoolSize(1)
              .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
              .withSSLContext(sslServerContext)
              .withStartTLS()
              .withServiceName("svc")
              .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
              .withoutAutoPublish()
              .withTCP()
              .withClientMode()
              .withWorkerThreadIoStrategy()
              .withSelectorThreadPoolSize(1)
              .withWorkerThreadPoolSize(1)
              .withSSLContext(sslClientContext)
              .withStartTLS()
              .withServiceName("clnt")
              .build();
        clnt.start();

        var clnt2 = new OncRpcSvcBuilder()
              .withoutAutoPublish()
              .withTCP()
              .withClientMode()
              .withWorkerThreadIoStrategy()
              .withSelectorThreadPoolSize(1)
              .withWorkerThreadPoolSize(1)
              .withServiceName("clnt2")
              .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
              .build();
        clnt2.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();

        XdrString s = new XdrString("hello TLS");
        XdrString reply = new XdrString();
        clntCall.call(ECHO, s, reply);

        assertEquals("reply mismatch", s, reply);

        var t2 = clnt2.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        var call2 = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t2);

        s = new XdrString("hello PLAIN");

        // trigger callback as TLS initialized by client. we talk plain, thus server might switch to TLS
        call2.call(UPPER, s, reply);

        call2.call(ECHO, s, reply);
        assertEquals("reply mismatch", s, reply);
    }

    @Test(expected = OncRpcRejectedException.class)
    public void shouldRejectStartTlsWhenNotConfigured() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withSSLContext(sslClientContext)
                .withStartTLS()
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
        clntCall.startTLS();
    }

    @Test(expected = EOFException.class) // rfc8446#section-6.2
    public void shouldRejectTlsWhenClientCertRequired() throws IOException, Exception {

        SSLParameters parameters = new SSLParameters();
        parameters.setNeedClientAuth(true);

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withSSLContext(sslServerContext)
                .withSSLParameters(parameters)
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withSSLContext(sslClientContext)
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);

        clntCall.call(NULL, XdrVoid.XDR_VOID, new XdrVoid());
    }

    public static SSLContext createClientSslContext(Certificate certificate) throws Exception {

        char[] password = "password".toCharArray();

        // create emtpy keystore and put certificates into it
        KeyStore keyStore = createEmptyKeystore();
        keyStore.setEntry("chain", new KeyStore.TrustedCertificateEntry(certificate),null);

        KeyManagerFactory keyManagerFactory
                = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        TrustManagerFactory trustManagerFactory
                = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());

        return sslContext;
    }

    public static SSLContext createServerSslContext(Certificate certificate, PrivateKey privateKey) throws Exception {

        char[] password = "password".toCharArray();

        Certificate[] certificateChain = {certificate};

        // create emtpy keystore and put certificates into it
        KeyStore keyStore = createEmptyKeystore();
        keyStore.setKeyEntry("private", privateKey, password, certificateChain);
        keyStore.setCertificateEntry("cert", certificate);

        KeyManagerFactory keyManagerFactory
                = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        TrustManagerFactory trustManagerFactory
                = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());

        return sslContext;
    }

    private static Certificate generateSelfSignedCert(KeyPair keyPair) throws GeneralSecurityException, OperatorCreationException {

        long notBefore = System.currentTimeMillis();
        long notAfter = notBefore + TimeUnit.DAYS.toMillis(1);

        X500Name subjectDN = new X500Name("CN=localhost, O=dCache.org");
        X500Name issuerDN = subjectDN;

        SubjectPublicKeyInfo subjectPublicKeyInfo =
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(issuerDN,
                BigInteger.ONE,
                new Date(notBefore),
                new Date(notAfter), subjectDN,
                subjectPublicKeyInfo);

        String signatureAlgorithm = "SHA256WithRSA";

        // sign with own key
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                .build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);
        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    private static KeyStore createEmptyKeystore() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}
