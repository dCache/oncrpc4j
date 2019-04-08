package org.dcache.oncrpc4j.benchmarks;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.rpc.RpcDispatchable;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 *
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
public class TlsOverhead {

    private static final int PROGNUM = 100017;
    private static final int PROGVER = 1;

    @Param({"true", "false"})
    private String withTLS;

    private OncRpcSvc svc;
    private OncRpcSvc clnt;
    private RpcCall clntCall;
    private SSLContext sslContext;

    private final RpcDispatchable NULL = (RpcCall call) -> call.reply(XdrVoid.XDR_VOID);

    @Setup
    public void setUp() throws IOException, Exception {

        if (Boolean.getBoolean(withTLS)) {
            sslContext = createSslContext();
        }

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
                .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), NULL)
                .withSSLContext(sslContext)
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
                .withSSLContext(sslContext)
                .withServiceName("clnt")
                .build();
        clnt.start();

        RpcTransport t = clnt.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
    }

    @TearDown
    public void shutdown() throws IOException {
        clnt.stop();
        svc.stop();
    }

    @Benchmark
    public XdrAble callOpNull() throws IOException {

        XdrVoid reply = new XdrVoid();
        clntCall.call(0, XdrVoid.XDR_VOID, reply);
        return reply;
    }


    public static SSLContext createSslContext() throws Exception {

        char[] password = "password".toCharArray();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Certificate certificate = generateSelfSignedCert(keyPair);
        Certificate[] certificateChain = {certificate};

        // create emtpy keystore and put certificates into it
        KeyStore keyStore = createEmptyKeystore();
        keyStore.setKeyEntry("private", keyPair.getPrivate(), password, certificateChain);
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

        SubjectPublicKeyInfo subjectPublicKeyInfo
                = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

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
