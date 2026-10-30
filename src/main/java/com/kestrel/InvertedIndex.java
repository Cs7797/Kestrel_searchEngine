package com.kestrel;


import java.util.*;

public class InvertedIndex {
    private final Map<String, Map<String, List<Integer>>> invertedIndex; 
    private final Map<String, Document> documents;
    private final Map<String, Integer> documentLengths;
    public InvertedIndex() {
        this.invertedIndex = new HashMap<>();
        this.documents = new HashMap<>();
        this.documentLengths = new HashMap<>();
    }
    
    private String[] tokenize(String content) {
    if (content == null || content.isBlank()) {
        return new String[0];
    }

    List<String> terms = new ArrayList<>();
    StringBuilder word = new StringBuilder();

    content = content.toLowerCase(Locale.ROOT);

    for (int i = 0; i < content.length(); i++) {
        char current = content.charAt(i);

        if (Character.isLetterOrDigit(current)) {
            word.append(current);
        } else {
            if (word.length() > 0) {
                terms.add(word.toString());
                word.setLength(0);
            }
        }
    }

    if (word.length() > 0) {
        terms.add(word.toString());
    }

    return terms.toArray(new String[terms.size()]);
}

public void addDocument(Document document) {
    Objects.requireNonNull(document, "document must not be null");

    Document oldDocument = documents.get(document.getId());

        if (oldDocument != null) {
            removeDocumentFromIndex(oldDocument);
             documentLengths.remove(document.getId());
        }

        documents.put(document.getId(), document);
       String[] terms = tokenize(document.getContent());
        documentLengths.put(document.getId(), terms.length);
        for (int position = 0; position < terms.length; position++) {
            String term = terms[position];

            Map<String, List<Integer>> postings = invertedIndex.computeIfAbsent(
                    term, key -> new HashMap<>());

            List<Integer> positions = postings.computeIfAbsent(
                    document.getId(), key -> new ArrayList<>());

            positions.add(position);
        }

    }
    

    public void removeDocumentFromIndex(Document document) {
        
    String[] terms = tokenize(document.getContent());

    for (String term : terms) {
        Map<String, List<Integer>> postings = invertedIndex.get(term);

        if (postings != null) {
            postings.remove(document.getId());

            if (postings.isEmpty()) {
                invertedIndex.remove(term);
            }
        }
    }
}
    
public void removeDocument(String documentId) {
    Document document = documents.get(documentId);

    if (document == null) {
        return;
    }

    removeDocumentFromIndex(document);
    documents.remove(documentId);
    documentLengths.remove(documentId);
}

   public List<String> search(String term) {
    if (term == null || term.isBlank()) {
        return List.of();
    }

    Map<String, List<Integer>> postings =
            invertedIndex.get(term.toLowerCase(Locale.ROOT));

    if (postings == null) {
        return List.of();
    }

    return new ArrayList<>(postings.keySet());
}

public int getTermFrequency(String term, String documentId) {
    if (term == null || term.isBlank()
            || documentId == null || documentId.isBlank()) {
        return 0;
    }

    Map<String, List<Integer>> postings = invertedIndex.get(term.toLowerCase(Locale.ROOT));

    if (postings == null) {
        return 0;
    }

    List<Integer> positions = postings.get(documentId);

    return positions == null ? 0 : positions.size();
}

public List<String> searchPhrase(String phrase) {
    if (phrase == null || phrase.isBlank()) {
        return List.of();
    }

    String[] terms = tokenize(phrase);

    if (terms.length == 0) {
        return List.of();
    }

    Map<String, List<Integer>> firstPostings = invertedIndex.get(terms[0]);

    if (firstPostings == null) {
        return List.of();
    }

    List<String> results = new ArrayList<>();

    for (String documentId : firstPostings.keySet()) {
        List<Integer> firstPositions = firstPostings.get(documentId);

        for (int startPosition : firstPositions) {
            boolean matches = true;

            for (int i = 1; i < terms.length; i++) {
                Map<String, List<Integer>> postings = invertedIndex.get(terms[i]);

                if (postings == null) {
                    matches = false;
                    break;
                }

                List<Integer> positions = postings.get(documentId);

                if (positions == null ||
                        !positions.contains(startPosition + i)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                results.add(documentId);
                break;
            }
        }
    }

    return results;
}
private int getDocumentLength(Document document) {
    return documentLengths.getOrDefault(document.getId(), 0);
}

private double getAverageDocumentLength() {
      if (documentLengths.isEmpty()) {
        return 0.0;
    }

    int totalLength = 0;

    for (int length : documentLengths.values()) {
        totalLength += length;
    }

    return (double) totalLength / documentLengths.size();
}
private double scoreTerm(String term, Document document,double averageLength) {
    Map<String, List<Integer>> postings = invertedIndex.get(term);

    if (postings == null || documents.isEmpty()) {
        return 0.0;
    }

    List<Integer> positions = postings.get(document.getId());

    if (positions == null || positions.isEmpty()) {
        return 0.0;
    }

    int frequency = positions.size();
    int documentCount = documents.size();
    int documentFrequency = postings.size();

    double idf = Math.log(
            1.0 + (documentCount - documentFrequency + 0.5)
                    / (documentFrequency + 0.5)
    );

    double k1 = 1.5;
    double b = 0.75;                            
    
    int documentLength = getDocumentLength(document);

    double lengthNormalization = averageLength == 0.0
            ? 0.0
            : 1.0 - b + b * documentLength / averageLength;

    return idf * (frequency * (k1 + 1.0))
            / (frequency + k1 * lengthNormalization);
}


   
public List<String> searchRanked(String query) {
    if (query == null || query.isBlank() || documents.isEmpty()) {
        return List.of();
    }

    String[] terms = tokenize(query);

    if (terms.length == 0) {
        return List.of();
    }

    // Collect only documents containing at least one query term.
    Set<String> candidates = new HashSet<>();

    for (String term : terms) {
        Map<String, List<Integer>> postings = invertedIndex.get(term);

        if (postings != null) {
            candidates.addAll(postings.keySet());
        }
    }

    if (candidates.isEmpty()) {
        return List.of();
    }

    double averageLength = getAverageDocumentLength();
    Map<String, Double> scores = new HashMap<>();

    // Score only candidate documents, not the entire collection.
    for (String documentId : candidates) {
        Document document = documents.get(documentId);

        if (document == null) {
            continue;
        }

        double totalScore = 0.0;

        for (String term : terms) {
            totalScore += scoreTerm(term, document, averageLength);
        }

        if (totalScore > 0.0) {
            scores.put(documentId, totalScore);
        }
    }

    return scores.entrySet()
            .stream()
            .sorted((a, b) -> {
                int comparison =
                        Double.compare(b.getValue(), a.getValue());

                return comparison != 0
                        ? comparison
                        : a.getKey().compareTo(b.getKey());
            })
            .map(Map.Entry::getKey)
            .toList();
}

}