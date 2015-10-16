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
import org.junit.After;
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
        Main main = new Main();
        vertx.deployVerticle(main);
    }

    @After
    public void tearDown() throws Exception {
        vertx.close();
        System.out.println("Vert.x Shutdown");
    }

    @Test
    public void testAddValidEntry(TestContext context) {
        final Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.putAbs("http://localhost:9080/rest/entry");
        req.putHeader("Content-Type", "application/json");
        req.putHeader("Accept", "*/*");
        req.handler((HttpClientResponse response) -> {
            context.assertNotNull(response);
            context.assertTrue(response.statusCode() == 202);
            async.complete();
        });
        req.exceptionHandler(exception -> {
            context.fail(exception.getLocalizedMessage());
            async.complete();
        });
        req.end("{\"given_name\": \"Lacy\", \"family_name\": \"Davis\"}");
    }

    @Test
    public void testGetWinner(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient()
                .get(9080, "localhost", "/rest/winner")
                .putHeader("Accept", "*/*")
                .handler(resp -> {
                    context.assertNotNull(resp);
                    context.assertEquals(resp.statusCode(), 200);
                    async.complete();
                })
                .exceptionHandler(err -> {
                    context.fail(err.getLocalizedMessage());
                    async.complete();
                })
                .end();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.putAbs("http://localhost:9080/rest/entry");
        req.putHeader("Content-Type", "application/json");
        req.putHeader("Accept", "*/*");
        req.handler(response -> {
            context.assertNotNull(response);
            vertx.createHttpClient()
                    .get(9080, "localhost", "/rest/winner")
                    .putHeader("Accept", "*/*")
                    .handler(resp -> {
                        context.assertNotNull(resp);
                        async.complete();
                    })
                    .exceptionHandler(err -> {
                        context.fail(err.getLocalizedMessage());
                        async.complete();
                    })
                    .end();
        });
        req.exceptionHandler(err -> {
            context.fail(err.getLocalizedMessage());
            async.complete();
        });
        req.end("{\"given_name\": \"Deven\", \"family_name\": \"Phillips\"}");
    }

    @Test
    public void testAddInvalidEntry(TestContext context) {
        final Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.putAbs("http://localhost:9080/rest/entry");
        req.putHeader("Content-Type", "application/json");
        req.putHeader("Accept", "*/*");
        req.handler(response -> {
            context.assertNotNull(response);
            async.complete();
        });
        req.exceptionHandler(err -> {
            context.fail(err.getLocalizedMessage());
            async.complete();
        });
        req.end("{\"given_nOME\": \"Deven\", \"family_nOME\": \"Phillips\"}");
    }
}
