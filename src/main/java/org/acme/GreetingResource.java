package org.acme;

import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Path("/hello")
public class GreetingResource {
    @Inject
    DataSource dataSource;
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

        if ("rollback".equals(message)) {
            transactionManager.setRollbackOnly();
            return "rollback message";
        } else {
            return "insert " + message + " ok";
        }
    }
}
