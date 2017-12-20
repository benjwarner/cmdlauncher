package org.tools4j.tabular.service;

import org.tools4j.tabular.util.PropertiesRepo;

import java.util.Map;

/**
 * User: ben
 * Date: 26/10/17
 * Time: 5:31 PM
 */
public class EnvironmentVariables {
    public PropertiesRepo load(){
        final Map<String, String> envVars = System.getenv();
        return new PropertiesRepo(envVars);
    }
}
