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
package org.dcache.xdr;

import java.io.IOException;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringRunner {

    private SpringRunner() {
        // this class it used only to bootstrap the Spring IoC
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: SpringRunner <config>");
            System.exit(1);
        }

        ConfigurableApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext(args[0]);

            OncRpcSvc service = (OncRpcSvc) context.getBean("oncrpcsvc");
            service.start();

            System.in.read();
        } catch (BeansException e) {
            System.err.println("Spring: " + e.getMessage());
            System.exit(1);
        } finally {
            if (context != null) {
                context.close();
            }
        }

        System.exit(0);
    }
}
