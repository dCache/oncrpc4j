package org.dcache.oncrpc4j.rpcgen.spoon;

import static spoon.testing.Assert.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dcache.xdr.RpcAuth;
import org.junit.BeforeClass;
import org.junit.Test;

import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.TypeFactory;

/**
 * This class will generate rpc java files in the RELATIVE_TGT_PATH directory
 * (target/generated-test-sources/); This directory will be cleaned after tests
 * runs (@see cleanRootDirectory).
 * 
 * Tests are created by calling predefined execution of the exec plugin
 * 
 * @see pom.xml
 * 
 *      Remark: This setup should be abstracted as an annotation taking
 *      <ul>
 *      <li>as mandator parameter the maven target to execute;</li>
 *      <li>as optional parameter the root dir to use</li>
 *      </ul>
 */
public class SpoonTest {
    private static final String M2_HOME = "/usr/share/maven/";
    private static final String POM_XML = "pom.xml";
    private static final String MAVEN_TARGET = "exec:java@'generate client and server calculator for testing'";

    private static final String GEN_TOP_DIR = "target/generated-test-sources/rpc";
    private static final String JAVA_FILE_DIR = "target/generated-test-sources/rpc/org/dcache/oncrpc4j/rpcgen";

    private static TypeFactory ftype = new TypeFactory();;

    private static Launcher spoon;

    private static final String getJavaFilePath(String name) {
        return JAVA_FILE_DIR + File.separator + name;
    }

    /**
     * Call maven task to generate java code before running test cases
     * 
     * @throws MavenInvocationException
     */

    @BeforeClass
    public static void invokeMavenTarget() throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        File pomFile = new File(POM_XML);
        assertTrue(pomFile.exists());
        request.setPomFile(pomFile);
        request.setGoals(Collections.singletonList(MAVEN_TARGET));
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(M2_HOME));
        invoker.execute(request);

        File calculatorJavaCode = new File(getJavaFilePath("Calculator.java"));
        assertTrue(calculatorJavaCode.exists());

        spoon = new Launcher();
        spoon.addInputResource("target/generated-test-sources/rpc");
        spoon.run();

    }

    /**
     * Test if a declaration named add_1 constant exists in the Generated class
     * Calculator.java
     */
    @Test
    public void addv1CallConstantExists() {
        // Check existence of class Calculator
        CtType<Object> type = spoon.getFactory().Type().get("org.dcache.oncrpc4j.rpcgen.Calculator");
        assertNotNull(type);

        // Check declaration of constant Calculator.add_1 
        assertThat(type.getField("add_1")).isEqualTo("public static final int add_1 = 1;");
    }

    /**
     * Test if the generated add_1 method in CalculatorClient.java is (String)
     * equals to src/test/resources/add_1.result file content
     * 
     */
    @Test
    public void addv1CallExists() throws FileNotFoundException, IOException {
        // Check existence of class CalculatorClient
        CtType<Object> type = spoon.getFactory().Type().get("org.dcache.oncrpc4j.rpcgen.CalculatorClient");
        assertNotNull(type);

        CtType<Object> typeCalcResult = spoon.getFactory().Type().get("org.dcache.oncrpc4j.rpcgen.CalculationResult");
        assertNotNull(typeCalcResult);
        
        // Check existence of method CalculatorClient.add_1 with
        // some specified profile exists
        CtMethod<Object> addProcImpl = type.getMethod(
                typeCalcResult.getReference(), "add_1", ftype.LONG_PRIMITIVE,
                ftype.LONG_PRIMITIVE, ftype.LONG_PRIMITIVE, ftype.get(TimeUnit.class).getReference(),
                ftype.get(RpcAuth.class).getReference());
        assertNotNull(addProcImpl);

        // Check if string spoon representation of CalculatorClient.add_1
        // is equals to content of the filef resources/add_1.result

        assertNotNull(this.getClass().getClassLoader().getResource("add_1.result"));
        File expectedResultCodeFile = new File(this.getClass().getClassLoader().getResource("add_1.result").getPath());
        String expectedResult = FileUtils.readFileToString(expectedResultCodeFile);

        assertThat(addProcImpl).isEqualTo(expectedResult);

    }

    /**
     * 
     * This test demonstrates how spoon may be used to compare generated code to
     * an expected result.
     * 
     * Expected result should be (minimally) defined in src/test/resources.
     * Generated result is created using maven
     * 
     * Both source code are analyzed and then their (String) representations are
     * compared
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void addv1CallExists2() throws FileNotFoundException, IOException {
        // get the generated method CalculatorClient.add_1
        CtType<Object> type = spoon.getFactory().Type().get("org.dcache.oncrpc4j.rpcgen.CalculatorClient");
        CtMethod<?> addProcImpl = type.getMethodsByName("add_1").get(0);
        
        //launch a new spoon analyzer process on the expected result file
        String expectedResultPath = this.getClass().getClassLoader()
                .getResource("org/dcache/oncrpc4j/rpcgen/ExpectedCalculatorClient.java").getPath();
        String expectedResult = FileUtils.readFileToString(new File(expectedResultPath));
        
        Launcher spoon2 = new Launcher();
        spoon2.addInputResource(expectedResultPath);
        spoon2.run();
        
        // get the expected add_1 method 
        List<CtClass> res = spoon2.getModel().getRootPackage()
                .filterChildren((CtClass clazz) -> clazz.getSimpleName().equals("ExpectedCalculatorClient")).list();
        assertEquals(1, res.size());
        CtType<Object> type2 = spoon2.getFactory().Type().get("org.dcache.oncrpc4j.rpcgen.ExpectedCalculatorClient");
        CtMethod<?> addProcImpl2 = type.getMethodsByName("add_1").get(0);
        assertNotNull(addProcImpl2);
        
        //compare the two spoon representations
        assertEquals(addProcImpl.toString(), addProcImpl2.toString());

    }

}
