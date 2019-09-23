package org.keycloak.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.keycloak.example.ejb.HelloBean;
import org.keycloak.example.ejb.KeycloakToken;
import org.keycloak.example.ejb.RemoteHello;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 *
 */
public class RemoteEjbClient {

    private static Logger logger;
    
    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        logger = Logger.getLogger(RemoteEjbClient.class);
    }
    
    public static void main( String[] args ) throws Exception {
        // Step 1 : Retrieve username+password of user. It can be done anyhow by the application (eg. swing form)
        UsernamePasswordHolder usernamePassword = promptUsernamePassword();
        //UsernamePasswordHolder usernamePassword = new UsernamePasswordHolder("john", "password");

        logger.infof("Will authenticate with username '%s' and password '%s'", usernamePassword.username, usernamePassword.password);

        // Step 2 : Elytron client security
        AuthenticationConfiguration user = AuthenticationConfiguration.empty()
                .setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("PLAIN"))
                .useName(usernamePassword.username)
                .usePassword(usernamePassword.password);
        
        final AuthenticationContext authCtx = AuthenticationContext.empty().with(MatchRule.ALL, user);

        AuthenticationContext.getContextManager().setThreadDefault(authCtx);
        
        // Step 3 : Keycloak DirectGrant (OAuth2 Resource Owner Password Credentials Grant) from the application
        DirectGrantInvoker directGrant = new DirectGrantInvoker();
        KeycloakToken keycloakToken = directGrant.keycloakAuthenticate(usernamePassword.username, usernamePassword.password);
        logger.info("Successfully authenticated against Keycloak and retrieved token");

        // Step 4 : Push credentials to clientContext from where ClientInterceptor can retrieve them
        SecurityActions.securityContextSetPrincipalCredential(null, keycloakToken);
        try {

            // Step 4 : EJB invoke
            final RemoteHello remoteHello = lookupRemoteStatelessHello();
            logger.info("Obtained RemoteHello for invocation");

            logger.info("Going to invoke EJB");
            String hello = remoteHello.helloSimple();
            logger.infof("HelloSimple invocation: %s", hello);

            String hello2 = remoteHello.helloAdvanced();
            logger.infof("HelloAdvanced invocation: %s", hello2);
        } finally {
            SecurityActions.clearSecurityContext();
        }
    }


    private static UsernamePasswordHolder promptUsernamePassword() throws IOException {
        logger.info("Remote EJB client will ask for your username and password and then authenticate against Keycloak and call EJB.");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            logger.info("Enter Username: ");
            String username = reader.readLine();
            logger.info("Enter Password: ");
            String password = reader.readLine();

            return new UsernamePasswordHolder(username, password);
        } finally {
            reader.close();
        }
    }


    /**
     * Looks up and returns the proxy to remote stateless calculator bean
     *
     * @return
     * @throws NamingException
     */
    private static RemoteHello lookupRemoteStatelessHello() throws NamingException {
        final Hashtable<String, Object> jndiProperties = new Hashtable<String, Object>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProperties.put(Context.PROVIDER_URL, "http://localhost:8080/wildfly-services");
        final Context context = new InitialContext(jndiProperties);
        try {
            // The app name is the application name of the deployed EJBs. This is typically the ear name
            // without the .ear suffix. However, the application name could be overridden in the application.xml of the
            // EJB deployment on the server.
            // Since we haven't deployed the application as a .ear, the app name for us will be an empty string
            final String appName = "";
            // This is the module name of the deployed EJBs on the server. This is typically the jar name of the
            // EJB deployment, without the .jar suffix, but can be overridden via the ejb-jar.xml
            // In this example, we have deployed the EJBs in a jboss-as-ejb-remote-app.jar, so the module name is
            // jboss-as-ejb-remote-app
            final String moduleName = "ejb-module";
            // AS7 allows each deployment to have an (optional) distinct name. We haven't specified a distinct name for
            // our EJB deployment, so this is an empty string
            final String distinctName = "";
            // The EJB name which by default is the simple class name of the bean implementation class
            final String beanName = HelloBean.class.getSimpleName();
            // the remote view fully qualified class name
            final String viewClassName = RemoteHello.class.getName();
            // let's do the lookup
            String lookupKey = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName;
            logger.infof("Lookup for remote EJB bean: %s", lookupKey);
            return (RemoteHello) context.lookup(lookupKey);
        } finally {
            context.close();
        }

    }


    private static class UsernamePasswordHolder {

        private final String username;
        private final String password;

        public UsernamePasswordHolder(String username, String password) {
            this.username = username;
            this.password = password;
        }

    }

}
