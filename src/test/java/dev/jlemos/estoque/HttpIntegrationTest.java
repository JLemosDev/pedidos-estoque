package dev.jlemos.estoque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpIntegrationTest {
    @LocalServerPort int porta;

    HttpResponse<String> enviar(String path, String json, boolean autenticado) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + path));
        if (autenticado) request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString("teste:teste12345".getBytes(StandardCharsets.UTF_8)));
        if (json != null) request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void interfaceTemEstiloEScriptAntesDoLogin() throws Exception {
        var css = enviar("/app.css", null, false);
        assertEquals(200, css.statusCode());
        assertTrue(css.body().contains(".sidebar"));
        assertTrue(css.headers().firstValue("content-type").orElse("").contains("text/css"));
        assertEquals(200, enviar("/app.js", null, false).statusCode());
    }

    @Test void apiExigeAutenticacao() throws Exception {
        assertEquals(401, enviar("/api/produtos", null, false).statusCode());
        assertEquals(200, enviar("/api/produtos", null, true).statusCode());
    }

    @Test void apiRejeitaProdutoInvalido() throws Exception {
        var response = enviar("/api/produtos", "{\"sku\":\"A\",\"nome\":\"X\",\"preco\":-1,\"estoque\":-2}", true);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("fields"));
    }
}
