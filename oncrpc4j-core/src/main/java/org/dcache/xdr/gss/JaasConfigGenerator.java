/*
 * Copyright (c) 2009 - 2015 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.xdr.gss;

import com.google.common.io.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class to generate JAAS configuration file for kerberized service.
 *
 * @since 2.3
 */
class JaasConfigGenerator {

    private final static String jaasConfigTemplate = "com.sun.security.jgss.accept {\n"
            + "  com.sun.security.auth.module.Krb5LoginModule required\n"
            + "  debug=false\n"
            + "  principal=\"%s\"\n"
            + "  doNotPrompt=true\n"
            + "  useKeyTab=true\n"
            + "  keyTab=\"%s\"\n"
            + "  debug=false\n"
            + "  isInitiator=false\n"
            + "  storeKey=true;\n"
            + "};";

    public static String generateJaasConfig(String servicePrincipal, String keytab) throws IOException {

        File jaasFile = File.createTempFile("jaas", ".conf");
        jaasFile.deleteOnExit();
        jaasFile.setExecutable(false);
        jaasFile.setReadable(true, true);
        jaasFile.setWritable(true, true);

        String config = String.format(jaasConfigTemplate, servicePrincipal, keytab);
        try (BufferedWriter bw = Files.newWriter(jaasFile, StandardCharsets.UTF_8)) {
            bw.write(config, 0, config.length());
        }
        return jaasFile.getAbsolutePath();
    }
}
