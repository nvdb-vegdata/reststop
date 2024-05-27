/*
 * Copyright 2018 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.vegvesen.nvdb.reststop.helloworld;

import jakarta.validation.ConstraintViolationException;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.kantega.reststop.helloworld.Utils.readPort;

/**
 *
 */
public class HelloworldIT {

    private final HttpClient client  = HttpClient.newHttpClient();

    @Test
    public void shouldReturnHelloWorld() throws IOException, URISyntaxException {
        String reststopPort = readPort();
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:" + reststopPort + "/helloworld/en?yo=hello").toURL().openConnection();
        connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary("joe:joe".getBytes("utf-8")));
        connection.setRequestProperty("Accept", "application/json");
        String message = IOUtils.toString(connection.getInputStream());

        assertThat(message, is("{\"message\":\"Hello world\"}"));
    }

    @Test
    public void shouldFail() throws IOException, URISyntaxException, InterruptedException {
        String reststopPort = readPort();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + reststopPort + "/helloworld/en"))
                .header("Authorization", "Basic " + DatatypeConverter.printBase64Binary("joe:joe".getBytes("utf-8")))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        System.out.println(res.body());

        assertEquals(400, res.statusCode());
        assertEquals("""
                [{"message":"must not be null","messageTemplate":"{jakarta.validation.constraints.NotNull.message}","path":"HelloworldResource.hello.arg1","invalidValue":null}]""",
                res.body()
        );
    }
}
