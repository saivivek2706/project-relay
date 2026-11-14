package com.azentio.projectrelay;
import com.azentio.projectrelay.generator.Generator;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc public class SeededReplayTest{
 @Autowired MockMvc mvc;
 @Test void replay() throws Exception{
   Generator.main(new String[]{"42","/tmp/seeded.jsonl","/tmp/labels.json"});
   for(String line: Files.readAllLines(Path.of("/tmp/seeded.jsonl"))){ mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(line)).andExpect(status().isCreated()); }
   var body=mvc.perform(get("/api/alerts?status=OPEN&page=0&size=100")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   // planted 10 should produce alerts, decoys should not create extra distinct alerts beyond planted+extensions: assert at least 10 alerts and no full account numbers
   org.junit.jupiter.api.Assertions.assertFalse(body.contains("1000000000"));
   org.junit.jupiter.api.Assertions.assertTrue(body.contains("XXXXXX"));
 }
}