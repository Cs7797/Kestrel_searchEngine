package com.kestrel.datasets;



import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.kestrel.Document;

public class WikipediaDatasetLoader {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private record ArticleMetadata(
            String title,
            long page_id,
            String url,
            String filename,
            String license
    ) {}

    public static List<Document> load(Path directory)
            throws IOException {

        Path articlesDirectory = directory.resolve("articles");
        Path metadataFile = directory.resolve("metadata.json");

        if (!Files.isDirectory(articlesDirectory)) {
            throw new IOException(
                    "Articles directory not found: " + articlesDirectory
            );
        }

        if (!Files.isRegularFile(metadataFile)) {
            throw new IOException(
                    "Metadata file not found: " + metadataFile
            );
        }

        List<ArticleMetadata> metadata =
                OBJECT_MAPPER.readValue(
                        metadataFile.toFile(),
                        new TypeReference<List<ArticleMetadata>>() {}
                );

        List<Document> documents = new ArrayList<>();

        for (ArticleMetadata article : metadata) {

            if (article.title() == null
                    || article.filename() == null
                    || article.url() == null) {
                throw new IOException(
                        "Invalid metadata entry: " + article
                );
            }

            Path articlePath = articlesDirectory
                    .resolve(article.filename())
                    .normalize();

            // Prevent paths from escaping the articles directory.
            if (!articlePath.startsWith(articlesDirectory.normalize())) {
                throw new IOException(
                        "Invalid article filename: " + article.filename()
                );
            }

            if (!Files.isRegularFile(articlePath)) {
                throw new IOException(
                        "Article file not found: " + articlePath
                );
            }

            String content = Files.readString(articlePath);

            if (content.isBlank()) {
                throw new IOException(
                        "Article content is empty: " + article.filename()
                );
            }

            String id = "wiki-" + article.page_id();

            documents.add(new Document(
                    id,
                    article.title(),
                    content,
                    article.url()
            ));
        }

        return documents;
    }
}
