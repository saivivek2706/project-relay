package com.azentio.projectrelay;

import com.azentio.projectrelay.generator.Generator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SeededReplayTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void seededDatasetProducesExactlyTheLabelledChainsAndNoDecoyAlerts() throws Exception {
        Path transfers = Path.of("/tmp/seeded.jsonl");
        Path labels = Path.of("/tmp/labels.json");
        Generator.main(new String[]{"42", transfers.toString(), labels.toString()});

        Set<String> generatedIds = new HashSet<>();
        for (String line : Files.readAllLines(transfers)) {
            generatedIds.add(mapper.readTree(line).path("transferId").asText());
            mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(line))
                    .andExpect(status().isCreated());
        }

        JsonNode labelRoot = mapper.readTree(Files.readString(labels));
        Set<List<String>> expected = new HashSet<>();
        for (JsonNode chain : labelRoot.path("planted")) {
            List<String> ids = new ArrayList<>();
            chain.forEach(id -> ids.add(id.asText()));
            expected.add(List.copyOf(ids));
        }
        assertEquals(10, expected.size(), "generator must label ten distinct planted chains");

        JsonNode alertPage = mapper.readTree(mvc.perform(get("/api/alerts")
                .param("status", "OPEN").param("page", "0").param("size", "100"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        List<List<String>> detectedFromDataset = new ArrayList<>();
        for (JsonNode alert : alertPage.path("content")) {
            long alertId = alert.path("alertId").asLong();
            JsonNode detail = mapper.readTree(mvc.perform(get("/api/alerts/" + alertId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            List<String> ids = new ArrayList<>();
            detail.path("hops").forEach(hop -> ids.add(hop.path("transferId").asText()));
            if (ids.stream().anyMatch(generatedIds::contains)) detectedFromDataset.add(List.copyOf(ids));
        }

        long truePositives = expected.stream()
                .filter(chain -> java.util.Collections.frequency(detectedFromDataset, chain) == 1).count();
        long falsePositives = detectedFromDataset.size() - truePositives;
        long falseNegatives = expected.size() - truePositives;
        double precision = detectedFromDataset.isEmpty() ? 1.0 : (double) truePositives / detectedFromDataset.size();
        double recall = (double) truePositives / expected.size();
        System.out.printf("Seeded replay: TP=%d FP=%d FN=%d precision=%.2f%% recall=%.2f%%%n",
                truePositives, falsePositives, falseNegatives, precision * 100, recall * 100);
        for (List<String> chain : expected) {
            assertEquals(1, java.util.Collections.frequency(detectedFromDataset, chain),
                    "planted chain must have exactly one alert: " + chain);
        }
        assertEquals(0, falsePositives, "no decoy or background alert may be created");
        assertEquals(0, falseNegatives, "every planted chain must be detected");
        assertEquals(1.0, precision);
        assertEquals(1.0, recall);
    }
}
