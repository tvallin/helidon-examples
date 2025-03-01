/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

package io.helidon.microprofile.examples.cors;

import io.helidon.microprofile.cors.CrossOrigin;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

/**
 * A simple JAX-RS resource to greet you with CORS support.
 */
@Path("/greet")
@RequestScoped
public class GreetResource {

    /**
     * The greeting message provider.
     */
    private final GreetingProvider greetingProvider;

    /**
     * Using constructor injection to get a configuration property.
     * By default this gets the value from META-INF/microprofile-config
     *
     * @param greetingConfig the configured greeting message
     */
    @Inject
    public GreetResource(GreetingProvider greetingConfig) {
        this.greetingProvider = greetingConfig;
    }

    /**
     * Return a worldly greeting message.
     *
     * @return {@link GreetingMessage}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GreetingMessage getDefaultMessage() {
        return createResponse("World");
    }

    /**
     * Return a greeting message using the name that was provided.
     *
     * @param name the name to greet
     * @return {@link GreetingMessage}
     */
    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GreetingMessage getMessage(@PathParam("name") String name) {
        return createResponse(name);
    }

    /**
     * Set the greeting to use in future messages.
     *
     * @param message {@link GreetingMessage} containing the new greeting
     * @return {@link Response}
     */
    @Path("/greeting")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(name = "message",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.OBJECT, requiredProperties = { "message" })))
    @APIResponses({
            @APIResponse(name = "normal", responseCode = "204", description = "Greeting updated"),
            @APIResponse(name = "missing 'greeting'", responseCode = "400",
                    description = "JSON did not contain setting for 'greeting'")})
    public Response updateGreeting(GreetingMessage message) {

        if (message.getMessage() == null) {
            GreetingMessage entity = new GreetingMessage("No greeting provided");
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }

        greetingProvider.setMessage(message.getMessage());
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * CORS set-up for updateGreeting.
     */
    @OPTIONS
    @Path("/greeting")
    @CrossOrigin(value = {"http://foo.com", "http://there.com"},
            allowMethods = {HttpMethod.PUT})
    @APIResponses({
            @APIResponse(name = "normal", responseCode = "200", description = "Preflight request granted"),
            @APIResponse(name = "bad preflight", responseCode = "403",
                    description = "Preflight request denied")})
    public void optionsForUpdatingGreeting() {
    }

    /**
     * CORS set-up for getDefaultMessage.
     */
    @OPTIONS
    @CrossOrigin()
    public void optionsForRetrievingUnnamedGreeting() {
    }

    /**
     * CORS set-up for getMessage.
     */
    @OPTIONS
    @CrossOrigin()
    @Path("/{name}")
    public void optionsForRetrievingNamedGreeting() {
    }

    private GreetingMessage createResponse(String who) {
        String msg = String.format("%s %s!", greetingProvider.getMessage(), who);
        return new GreetingMessage(msg);
    }
}
