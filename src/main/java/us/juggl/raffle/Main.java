package us.juggl.raffle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.jdbc.JDBCDataSource;

/**
 *
 */
public class Main extends AbstractVerticle {
    private static final String SELECT_WINNER = "SELECT p.given_name, p.family_name, p.full_name FROM person p ORDER BY RAND() LIMIT 1";
    private static final String INSERT_ENTRY = "INSERT INTO person (given_name, family_name, full_name) VALUES (?, ?, ?)";
    private static final String JSON = "application/json";
    
    private final JDBCDataSource ds;
    private final Logger LOG;

    public static void main(String... args) throws Exception {
        Vertx.vertx().deployVerticle(new Main());
    }

    public Main() throws Exception {
        LOG = LoggerFactory.getLogger(this.getClass());
        ds = new JDBCDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:raffle");
        ds.setUser("SA");
        Database db = DatabaseFactory
            .getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(ds.getConnection()));
        Liquibase l = new Liquibase("liquibase.yml", new ClassLoaderResourceAccessor(), db);
        l.update((String) null);
    }

    @Override
    public void start() throws Exception {
        Router r = Router.router(vertx);
        r.route().handler(BodyHandler.create());
        r.get("/rest/winner").produces(JSON).blockingHandler(this::getWinner);
        r.put("/rest/entry").consumes(JSON).produces(JSON).blockingHandler(this::addEntry);
        r.route().handler(StaticHandler.create("webroot"));
        
        HttpServer server = vertx.createHttpServer().requestHandler(r::accept).listen(8080, "0.0.0.0");
    }
    
    private void getWinner(final RoutingContext rc) {
        JsonObject result = new JsonObject();
        try (Connection c = ds.getConnection();
            Statement s = c.createStatement();
            ResultSet r = s.executeQuery(SELECT_WINNER)) {
            if (r.isFirst() || r.next()) {
                result.put("given_name", r.getString("given_name"));
                result.put("family_name", r.getString("family_name"));
                result.put("full_name", r.getString("full_name"));
                sendResponse(rc, 200, "OK", result);
            } else {
                result.put("error", "No results");
                sendResponse(rc, 404, "Not Found", result);
            }
        } catch (SQLException sqle) {
            result.put("error", sqle.getLocalizedMessage());
            sendResponse(rc, 500, "Server Error", result);
            LOG.error("Error getting winner from DB", sqle);
        }
    }

    private void addEntry(final RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        LOG.debug("BODY: "+body.encodePrettily());

        try (Connection c = ds.getConnection();
            PreparedStatement s = c.prepareStatement(INSERT_ENTRY)) {
            body.put("full_name", body.getString("family_name")+", "+body.getString("given_name"));
            s.setString(1, body.getString("given_name"));
            s.setString(2, body.getString("family_name"));
            s.setString(3, body.getString("full_name"));
            s.executeUpdate();
            sendResponse(rc, 202, "Accepted", body);
        } catch (SQLException sqle) {
            JsonObject result = new JsonObject();
            result.put("error", sqle.getLocalizedMessage());
            sendResponse(rc, 400, "Bad Request", result);
        }
    }

    private void sendResponse(RoutingContext rc, int code, String msg, JsonObject body) {
                rc
                    .response()
                    .setStatusCode(code)
                    .setStatusMessage(msg)
                    .putHeader("Content-Type", JSON)
                    .end(body.encodePrettily());
    }
}
