/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Agent {

    public static void premain(String args, Instrumentation inst) throws Exception {
        openJvmciPackages(inst);

        if (args != null) {
            for (String className : args.split(",")) {
                Linker.linkClass(Class.forName(className));
            }
        }
    }

    private static void openJvmciPackages(Instrumentation inst) {
        Optional<Module> jvmciModule = ModuleLayer.boot().findModule("jdk.internal.vm.ci");
        if (jvmciModule.isEmpty()) {
            throw new IllegalStateException("JVMCI module not found. Use -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI");
        }

        Set<Module> unnamed = Set.of(
                ClassLoader.getPlatformClassLoader().getUnnamedModule(),
                ClassLoader.getSystemClassLoader().getUnnamedModule()
        );

        Map<String, Set<Module>> extraExports = Map.of(
                "jdk.vm.ci.code", unnamed,
                "jdk.vm.ci.code.site", unnamed,
                "jdk.vm.ci.hotspot", unnamed,
                "jdk.vm.ci.meta", unnamed,
                "jdk.vm.ci.runtime", unnamed
        );

        inst.redefineModule(jvmciModule.get(), Collections.emptySet(), extraExports,
                Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap());
    }
}
