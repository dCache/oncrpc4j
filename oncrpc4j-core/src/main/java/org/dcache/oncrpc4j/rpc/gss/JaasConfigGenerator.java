/*
 * Copyright (c) 2009 - 2018 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.rpc.gss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

/**
 * Utility class to generate JAAS configuration file for kerberized service.
 *
 * @since 2.3
 */
class JaasConfigGenerator {

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_RW =
            PosixFilePermissions.asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE));

    private final static String JAAS_CONFIG_TEMPLATE = "com.sun.security.jgss.accept {\n"
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

        Path jaasFile = Files.createTempFile("jaas", ".conf", OWNER_RW);
        jaasFile.toFile().deleteOnExit();

        String config = String.format(JAAS_CONFIG_TEMPLATE, servicePrincipal, keytab);
        Files.write(jaasFile, config.getBytes(StandardCharsets.UTF_8));

        return jaasFile.toAbsolutePath().toString();
    }
}
