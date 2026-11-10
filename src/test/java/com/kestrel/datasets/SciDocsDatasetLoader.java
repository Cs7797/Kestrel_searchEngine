
package com.kestrel.datasets;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kestrel.Document;

public class SciDocsDatasetLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Document> load(Path corpusPath) throws IOException {

        List<Document> documents = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(corpusPath)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                Map<String, Object> record = objectMapper.readValue(
                        line,
                        new TypeReference<Map<String, Object>>() {
                        });

                String id = (String) record.get("_id");
                String title = (String) record.get("title");
                String content = (String) record.get("text");

                if (id == null || title == null || content == null) {
                    throw new IllegalArgumentException(
                            "Invalid SciDocs record: missing required fields");
                }

                documents.add(
                        new Document(id, title, content, null));
            }
        }

        return documents;
    }
    public List<SciDocsQuery> loadQueries(Path queriesPath)
        throws IOException {

    List<SciDocsQuery> queries = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();

    try (var lines = Files.lines(queriesPath)) {
        for (String line : lines.toList()) {

            if (line.isBlank()) {
                continue;
            }

            Map<String, Object> record = mapper.readValue(
                    line,
                    new TypeReference<Map<String, Object>>() {}
            );

            String id = (String) record.get("_id");
            String queryText = (String) record.get("text");

            if (id == null || id.isBlank()
                    || queryText == null || queryText.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid SciDocs query record: " + line
                );
            }

            queries.add(new SciDocsQuery(id, queryText));
        }
    }

    return queries;
}


public Map<String, Map<String, Integer>> loadQrels(Path qrelsPath)
        throws IOException {

    Map<String, Map<String, Integer>> qrels = new HashMap<>();

    try (var lines = Files.lines(qrelsPath)) {

        for (String line : lines.skip(1).toList()) {

            if (line.isBlank()) {
                continue;
            }

            String[] columns = line.split("\t");

            if (columns.length != 3) {
                throw new IllegalArgumentException(
                        "Invalid qrels row: " + line
                );
            }

            String queryId = columns[0];
            String documentId = columns[1];
            int relevance = Integer.parseInt(columns[2]);

            qrels.computeIfAbsent(queryId, key -> new HashMap<>())
                    .put(documentId, relevance);
        }
    }

    return qrels;
}
}
