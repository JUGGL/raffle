/**
 * Copyright 2015 Joseph "Deven" Phillips
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package us.juggl.raffle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
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
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.jdbc.JDBCDataSource;

/**
 *
 */
public class Main extends AbstractVerticle {
    private static final String SELECT_WINNER = "SELECT p.given_name, p.family_name, p.full_name FROM person p ORDER BY RAND() LIMIT 1";
    private static final String INSERT_ENTRY = "INSERT INTO person (given_name, family_name, full_name) VALUES (?, ?, ?)";
    private static final String JSON = "application/json";
    private static final String GIVEN = "given_name";
    private static final String FAMILY = "family_name";
    private static final String FULL = "full_name";
    private static final String ERROR = "error";
    
    private final JDBCDataSource ds;
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * Initialize the main portions of the application and set up the DataSource
     * @throws LiquibaseException If there is a problem setting the database schema
     * @throws SQLException If there is a problem configuring the DataSource
     */
    public Main() throws LiquibaseException, SQLException {
        ds = new JDBCDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:raffle");
        ds.setUser("SA");
        Database db = DatabaseFactory
            .getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(ds.getConnection()));
        Liquibase l = new Liquibase("liquibase.yml", new ClassLoaderResourceAccessor(), db);
        l.update((String) null);
    }

    /**
     * Entrypoint for the application.
     * @param args Command-line arguments
     * @throws LiquibaseException If there is a problem setting the database schema
     * @throws SQLException If there is a problem configuring the DataSource
     */
    public static void main(String... args) throws LiquibaseException, SQLException {
        Vertx.vertx().deployVerticle(new Main());
    }

    @Override
    public void start() throws Exception {
        LOG.info("Starting Main Verticle");
        Router r = Router.router(vertx);
        r.route().handler(BodyHandler.create());
        r.get("/rest/winner").produces(JSON).blockingHandler(this::getWinner);
        r.put("/rest/entry").consumes(JSON).produces(JSON).blockingHandler(this::addEntry);
        r.route().handler(StaticHandler.create("webroot").setIndexPage("index.html"));
        
        vertx
            .createHttpServer()
            .requestHandler(r::accept)
            .listen(9080, "0.0.0.0", result -> {
                if (result.succeeded()) {
                    LOG.info("HTTP Server started");
                } else {
                    LOG.error("HTTP Server failed to start", result.cause());
                }
            });
        LOG.info("Main verticle started");
    }

    /**
     * Handler for GET requests to the '/rest/winner' URL. Returns a randomly selected person from the database in JSON
     * format.
     * @param rc The {@link RoutingContext} for the incoming request.
     */
    void getWinner(final RoutingContext rc) {
        JsonObject result = new JsonObject();
        try (Connection c = ds.getConnection();
            Statement s = c.createStatement();
            ResultSet r = s.executeQuery(SELECT_WINNER)) {
            if (r.isFirst() || r.next()) {
                result.put(GIVEN, r.getString(GIVEN));
                result.put(FAMILY, r.getString(FAMILY));
                result.put(FULL, r.getString(FULL));
                sendResponse(rc, 200, "OK", result);
            } else {
                result.put(ERROR, "No results");
                sendResponse(rc, 404, "Not Found", result);
            }
        } catch (SQLException sqle) {
            result.put(ERROR, sqle.getLocalizedMessage());
            sendResponse(rc, 500, "Server Error", result);
            LOG.error("Error getting winner from DB", sqle);
        }
    }

    /**
     * Handler for the PUT requests to '/rest/entry'. Adds a new entry to the Person table.
     * @param rc The {@link RoutingContext} for the incoming request.
     */
    void addEntry(final RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        LOG.debug("BODY: "+body.encodePrettily());

        try (Connection c = ds.getConnection();
            PreparedStatement s = c.prepareStatement(INSERT_ENTRY)) {
            if (body.getString(FAMILY)!=null && body.getString(GIVEN)!=null) {
                body.put(FULL, body.getString(FAMILY)+", "+body.getString(GIVEN));
                s.setString(1, body.getString(GIVEN));
                s.setString(2, body.getString(FAMILY));
                s.setString(3, body.getString(FULL));
                s.executeUpdate();
                sendResponse(rc, 202, "Accepted", body);
            } else {
                JsonObject result = new JsonObject();
                result.put(ERROR, "Request MUST provide 'given_name' and 'family_name' in the JSON body.");
                sendResponse(rc, 400, "Bad Request", result);
            }
        } catch (SQLException sqle) {
            LOG.error("SQLException", sqle);
            JsonObject result = new JsonObject();
            result.put(ERROR, sqle.getLocalizedMessage());
            sendResponse(rc, 400, "Bad Request", result);
        }
    }

    /**
     * Sends the appropriate response using the {@link RoutingContext}'s {@link io.vertx.core.http.HttpServerResponse}
     * @param rc The {@link RoutingContext} for the incoming request.
     * @param code The HTTP status code to be set
     * @param msg The HTTP status message to be set
     * @param body The {@link JsonObject} to be serialized and send as the response body.
     */
    void sendResponse(RoutingContext rc, int code, String msg, JsonObject body) {
                rc
                    .response()
                    .setStatusCode(code)
                    .setStatusMessage(msg)
                    .putHeader("Content-Type", JSON)
                    .end(body.encodePrettily());
    }
}
