/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.integrations.micrometer.se;

import java.util.Collections;

import io.helidon.config.Config;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRequest;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;

/**
 * A simple service to greet you.
 * <p>
 * Examples:
 * <pre>{@code
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * }</pre>
 * The greeting message is returned as a JSON object.
 *
 * </p>
 */

public class GreetService implements HttpService {

    /**
     * The config value for the key {@code greeting}.
     */
    private String greeting;

    private final Timer getTimer;
    private final Counter personalizedGetCounter;

    private static final JsonBuilderFactory JSON_BF = Json.createBuilderFactory(Collections.emptyMap());

    GreetService(Timer getTimer, Counter personalizedGetCounter) {
        Config config = Config.global();
        this.greeting = config.get("app.greeting").asString().orElse("Ciao");
        this.getTimer = getTimer;
        this.personalizedGetCounter = personalizedGetCounter;
    }

    /**
     * A service registers itself by updating the routine rules.
     * @param rules the routing rules.
     */
    @Override
    public void routing(HttpRules rules) {
        rules
            .get((req, resp) -> getTimer.record(resp::next)) // Update the timer with every GET.
            .get("/", this::getDefaultMessageHandler)
            .get("/{name}",
                    (req, resp) -> {
                            personalizedGetCounter.increment();
                            resp.next();
                        }, // Count personalized GETs...
                    this::getMessageHandler) // ...and process them.
            .put("/greeting", this::updateGreetingHandler);
    }

    /**
     * Return a worldly greeting message.
     * @param request the server request
     * @param response the server response
     */
    private void getDefaultMessageHandler(HttpRequest request,
                                   ServerResponse response) {
        sendResponse(response, "World");
    }

    /**
     * Return a greeting message using the name that was provided.
     * @param request the server request
     * @param response the server response
     */
    private void getMessageHandler(ServerRequest request,
                            ServerResponse response) {
        String name = request.path().pathParameters().first("name").get();
        sendResponse(response, name);
    }

    private void sendResponse(ServerResponse response, String name) {
        GreetingMessage msg = new GreetingMessage(String.format("%s %s!", greeting, name));
        response.send(msg.forRest());
    }

    private void updateGreetingFromJson(JsonObject jo, ServerResponse response) {

        if (!jo.containsKey(GreetingMessage.JSON_LABEL)) {
            JsonObject jsonErrorObject = JSON_BF.createObjectBuilder()
                    .add("error", "No greeting provided")
                    .build();
            response.status(Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        greeting = GreetingMessage.fromRest(jo).getMessage();
        response.status(Status.NO_CONTENT_204).send();
    }

    /**
     * Set the greeting to use in future messages.
     * @param request the server request
     * @param response the server response
     */
    private void updateGreetingHandler(ServerRequest request,
                                       ServerResponse response) {
        JsonObject obj = request.content().as(JsonObject.class);
        updateGreetingFromJson(obj, response);
    }
}
