package com.kestrel.datasets;

import org.junit.jupiter.api.Test;

import com.kestrel.Document;
import com.kestrel.InvertedIndex;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WikipediaDatasetLoaderTest {

//     @Test
//     void shouldLoadWikipediaDocuments() throws Exception {

//         Path directory = Path.of("datasets/wikipedia");

//         List<Document> documents = WikipediaDatasetLoader.load(directory);

//         assertFalse(documents.isEmpty());

//         assertNotNull(documents.get(0).getId());
//         assertNotNull(documents.get(0).getContent());

//         System.out.println(
//                 "Loaded Wikipedia documents: " + documents.size());
//     }
//     @Test
//     void shouldIndexAndSearchWikipediaDocuments() throws Exception {

//         Path directory = Path.of("datasets/wikipedia");

//         List<Document> documents = WikipediaDatasetLoader.load(directory);

//         InvertedIndex index = new InvertedIndex();

//         for (Document document : documents) {
//             index.addDocument(document);
//         }

//         assertFalse(index.searchRanked("Java").isEmpty());

//         System.out.println("Total indexed documents: " + documents.size());

//         List<String> results = index.searchRanked("Java");

//         System.out.println("Java search results: " + results);
//     }
// @Test


// void shouldRetrieveRelevantWikipediaArticles() throws Exception {

//     Path directory = Path.of("datasets/wikipedia");

//     List<Document> documents =
//             WikipediaDatasetLoader.load(directory);

//     InvertedIndex index = new InvertedIndex();

//     for (Document document : documents) {
//         index.addDocument(document);
//     }

//     // Find the Java article using its actual title
//     Document javaArticle = documents.stream()
//             .filter(document -> document.getTitle()
//                     .equals("Java (programming language)"))
//             .findFirst()
//             .orElseThrow();

//     List<String> javaResults = index.searchRanked("Java");

//     assertTrue(javaResults.contains(javaArticle.getId()));

//     // Test ranked search
//     List<String> machineLearningResults =
//             index.searchRanked("machine learning");

//     assertFalse(machineLearningResults.isEmpty());

//     // Test exact phrase search
//     List<String> phraseResults =
//             index.searchPhrase("machine learning");

//     assertFalse(phraseResults.isEmpty());

//     System.out.println("Java results: " + javaResults);
//     System.out.println("Machine learning results: "
//             + machineLearningResults);
//     System.out.println("Phrase search results: " + phraseResults);
// }

}