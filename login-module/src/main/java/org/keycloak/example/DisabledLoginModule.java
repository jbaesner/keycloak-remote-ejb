/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.logging.Logger;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * Due to JBEAP-16383 there's a need of providing some kind of 'dummy' authentication. This is being done
 * configuring this LoginModule as JAAS module like:
 * 
 * <pre>
 *      &lt;management&gt;
 *          &lt;security-realms&gt;
 *              ...
 *              &lt;security-realm name="DisabledRealm"&gt;
 *                  &lt;authentication&gt;
 *                      &lt;jaas name="disabled-security"/&gt;
 *                  &lt;/authentication&gt;
 *              &lt;/security-realm&gt;
 *          &lt;/security-realms&gt;
 *          ...
 *      &lt;/management&gt;
 *      ...
 *      &lt;subsystem xmlns="urn:jboss:domain:undertow:7.0" default-server="default-server" default-virtual-host="default-host" default-servlet-container="default" default-security-domain="other"&gt;
 *              ...
 *              &lt;host name="default-host" alias="localhost"&gt;
 *                  &lt;location name="/" handler="welcome-content"/&gt;
 *                  &lt;http-invoker security-realm="DisabledRealm"/&gt;
 *              &lt;/host&gt;
 *          &lt;/server&gt;
 *          ...
 *      &lt;/subsystem&gt;
 *      ...
 *      &lt;subsystem xmlns="urn:jboss:domain:security:2.0"&gt;
 *              ...
 *              &lt;security-domain name="disabled-security"&gt;
 *                  &lt;authentication&gt;
 *                      &lt;login-module code="org.keycloak.example.DisabledLoginModule" module="org.keycloak.example" flag="required" /&gt;
 *                  &lt;/authentication&gt;
 *              &lt;/security-domain&gt;
 *              ...
 *      &lt;/subsystem&gt;
 * </pre>
 * 
 * @author jbaesner
 *
 */
public class DisabledLoginModule extends AbstractServerLoginModule {

    private static final Logger logger = Logger.getLogger(DisabledLoginModule.class);

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {

        logger.warn("DisabledLoginModule is being used. This module does not perform any kind of authorization! It's being used to workaround JBEAP-16383!");
        
        super.login();
        loginOk = true;
        return true;
    }

    @Override
    protected Principal getIdentity() {
        return new SimplePrincipal("authentication-disabled");
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        Group[] roles = new Group[]{};
        return roles;
    }
}
