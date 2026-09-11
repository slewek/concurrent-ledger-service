package dev.challenge.ledger.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAccountsTransferMoneyAndReadBalance()
            throws Exception {

        String sourceId = createAccount(1_000);
        String destinationId = createAccount(0);

        mockMvc.perform(
                        post("/transfers")
                                .header(
                                        "Idempotency-Key",
                                        "transfer-api-test"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 300
                                        }
                                        """.formatted(
                                        sourceId,
                                        destinationId
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.amount")
                        .value(300));

        mockMvc.perform(
                        get("/accounts/{accountId}/balance", sourceId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance")
                        .value(700));

        mockMvc.perform(
                        get(
                                "/accounts/{accountId}/balance",
                                destinationId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance")
                        .value(300));
    }

    private String createAccount(long initialBalance)
            throws Exception {

        var response = mockMvc.perform(
                        post("/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "initialBalance": %d
                                        }
                                        """.formatted(initialBalance))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        return json.get("id").asText();
    }
}