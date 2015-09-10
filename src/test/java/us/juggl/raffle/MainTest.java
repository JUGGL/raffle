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

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by dphillips on 9/10/15.
 */
@RunWith(io.vertx.ext.unit.junit.VertxUnitRunner.class)
public class MainTest {
    private Vertx vertx;

    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();
        vertx.deployVerticle(new Main());
    }

    @Test
    public void testAddValidEntry(TestContext context) {
        final Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.put(8080, "127.0.0.1", "/rest/entry");
        req.handler((HttpClientResponse response) -> {
            context.assertNotNull(response);
            context.assertTrue(response.statusCode()==202);
            async.complete();
        });
        req.exceptionHandler(exception -> {
            context.fail(exception.getLocalizedMessage());
            async.complete();
        });
        req.end("{\"given_name\": \"Deven\", \"family_name\": \"Phillips\"}");
    }
    @Test
    public void testGetWinner(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().put(8080, "localhost", "/rest/entry").handler(response -> {
            context.assertNotNull(response);
            vertx.createHttpClient().get(8080, "localhost", "/rest/winner").handler(resp -> {
                context.assertNotNull(resp);
                async.complete();
            }).end();
        }).end("{\"given_name\": \"Deven\", \"family_name\": \"Phillips\"}");
    }

    @Test
    public void testAddInvalidEntry(TestContext context) {
        final Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.put(8080, "localhost", "/rest/entry");
        req.handler(response -> {
            context.assertNotNull(response);
            async.complete();
        });
        req.end("{\"given_nOME\": \"Deven\", \"family_nOME\": \"Phillips\"}");
    }
}
