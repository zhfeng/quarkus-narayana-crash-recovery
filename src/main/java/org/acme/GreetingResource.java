package org.acme;

import org.jboss.tm.XAResourceRecoveryRegistry;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static java.lang.Thread.sleep;

@Path("/hello")
public class GreetingResource {
    @Inject
    DataSource dataSource;

    @Inject
    XAResourceRecoveryRegistry xaResourceRecoveryRegistry;

    @Inject
    TransactionManager transactionManager;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String hello(String message) throws Exception {
        DummyXAResource xaResource = new DummyXAResource("crash".equals(message));
        transactionManager.getTransaction().enlistResource(xaResource);

        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(
                        "insert into audit_log (message) values ('" + message + "')");
            }

        }

        return "insert " + message + " ok";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/recovery")
    public void recovery() throws Exception {
        int entriesBefore = count();
        xaResourceRecoveryRegistry.addXAResourceRecovery(new DummyXAResourceRecovery());

        boolean isComplete = false;
        for (int i = 0; i < 3 && !isComplete; i++) {
            sleep(5000);
            int n = count();
            System.out.println("current audit_log entries: " + n);
            isComplete = entriesBefore < n;
        }

        if (isComplete) {
            System.out.println("Recovery completed successfully");
        } else {
            throw new Exception("Something wrong happened and recovery didn't complete");
        }
    }

    private int count() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("select count(*) from audit_log");
                while(rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
}