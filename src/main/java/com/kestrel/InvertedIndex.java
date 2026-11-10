
package com.kestrel;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InvertedIndex {

    // Field-specific positional indexes:
    // term -> document ID -> positions
    private final Map<String, Map<String, List<Integer>>> titleIndex;
    private final Map<String, Map<String, List<Integer>>> bodyIndex;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Document> documents;

    // Searchable lengths exclude stopwords.
    private final Map<String, Integer> titleLengths;
    private final Map<String, Integer> bodyLengths;

    private static final double DEFAULT_TITLE_BOOST = 2.0;
    private final double titleBoost;
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    public InvertedIndex() {
        this(DEFAULT_TITLE_BOOST);
    }

    /** Creates an index with an explicit title-field weight. */
    public InvertedIndex(double titleBoost) {
        if (!Double.isFinite(titleBoost) || titleBoost < 0.0) {
            throw new IllegalArgumentException("titleBoost must be finite and non-negative");
        }
        this.titleBoost = titleBoost;
        this.titleIndex = new HashMap<>();
        this.bodyIndex = new HashMap<>();
        this.documents = new HashMap<>();
        this.titleLengths = new HashMap<>();
        this.bodyLengths = new HashMap<>();
    }

    public void addDocument(Document document) {

        lock.writeLock().lock();

        try {
            Objects.requireNonNull(document, "document must not be null");

            if (documents.containsKey(document.getId())) {
                removeDocument(document.getId());
            }

            documents.put(document.getId(), document);

            addField(document.getId(), document.getTitle(), titleIndex);
            addField(document.getId(), document.getContent(), bodyIndex);

            titleLengths.put(
                    document.getId(),
                    TextNormalizer.searchableLength(document.getTitle()));

            bodyLengths.put(
                    document.getId(),
                    TextNormalizer.searchableLength(document.getContent()));

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addField(
            String documentId,
            String text,
            Map<String, Map<String, List<Integer>>> index) {

        List<String> terms = TextNormalizer.normalizeForIndex(text);

        // Preserve stopwords and positions for phrase searching.
        for (int position = 0; position < terms.size(); position++) {
            String term = terms.get(position);

            Map<String, List<Integer>> postings = index.computeIfAbsent(term, key -> new HashMap<>());

            List<Integer> positions = postings.computeIfAbsent(
                    documentId,
                    key -> new ArrayList<>());

            positions.add(position);
        }
    }

    private void removeField(
            String documentId,
            String text,
            Map<String, Map<String, List<Integer>>> index) {

        List<String> terms = TextNormalizer.normalizeForIndex(text);

        for (String term : terms) {
            Map<String, List<Integer>> postings = index.get(term);

            if (postings == null) {
                continue;
            }

            postings.remove(documentId);

            // Remove terms that no longer have any documents.
            if (postings.isEmpty()) {
                index.remove(term);
            }
        }
    }

    public void removeDocumentFromIndex(Document document) {

        lock.writeLock().lock();

        try {
            Objects.requireNonNull(document, "document must not be null");

            String documentId = document.getId();

            Document indexedDocument = documents.remove(documentId);

            Document documentToRemove = indexedDocument == null ? document : indexedDocument;

            removeField(
                    documentId,
                    documentToRemove.getTitle(),
                    titleIndex);

            removeField(
                    documentId,
                    documentToRemove.getContent(),
                    bodyIndex);

            titleLengths.remove(documentId);
            bodyLengths.remove(documentId);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            titleIndex.clear();
            bodyIndex.clear();
            documents.clear();
            titleLengths.clear();
            bodyLengths.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeDocument(String documentId) {

        lock.writeLock().lock();

        try {
            if (documentId == null || documentId.isBlank()) {
                return;
            }

            Document document = documents.get(documentId);

            if (document == null) {
                return;
            }

            removeDocumentFromIndex(document);

        } finally {
            lock.writeLock().unlock();
        }
    }

    // --------------------------------------------------
    // BASIC SEARCH
    // Searches both title and body.
    // --------------------------------------------------
    public List<String> search(String term) {
        lock.readLock().lock();

        try {
            String normalizedTerm = TextNormalizer.normalizeTerm(term);

            if (normalizedTerm == null || normalizedTerm.isBlank()) {
                return List.of();
            }

            Set<String> results = new HashSet<>();

            addPostingDocuments(titleIndex, normalizedTerm, results);
            addPostingDocuments(bodyIndex, normalizedTerm, results);

            return results.stream()
                    .sorted()
                    .toList();

        } finally {
            lock.readLock().unlock();
        }
    }

    private void addPostingDocuments(
            Map<String, Map<String, List<Integer>>> index,
            String term,
            Set<String> results) {

        Map<String, List<Integer>> postings = index.get(term);

        if (postings != null) {
            results.addAll(postings.keySet());
        }
    }

    public int getTermFrequency(String term, String documentId) {
        lock.readLock().lock();

        try {
            String normalizedTerm = TextNormalizer.normalizeTerm(term);

            if (normalizedTerm == null || normalizedTerm.isBlank()) {
                return 0;
            }

            int titleFrequency = getFieldTermFrequency(
                    titleIndex, normalizedTerm, documentId);

            int bodyFrequency = getFieldTermFrequency(
                    bodyIndex, normalizedTerm, documentId);

            return titleFrequency + bodyFrequency;

        } finally {
            lock.readLock().unlock();
        }
    }

    private int getFieldTermFrequency(
            Map<String, Map<String, List<Integer>>> index,
            String term,
            String documentId) {

        Map<String, List<Integer>> postings = index.get(term);

        if (postings == null) {
            return 0;
        }

        List<Integer> positions = postings.get(documentId);

        return positions == null ? 0 : positions.size();
    }

    public List<String> searchPhrase(String phrase) {
        lock.readLock().lock();

        try {
            if (phrase == null || phrase.isBlank()) {
                return List.of();
            }

            List<String> terms = TextNormalizer.normalizeForIndex(phrase);

            if (terms.isEmpty()) {
                return List.of();
            }

            Set<String> results = new HashSet<>();

            results.addAll(searchPhraseInField(terms, titleIndex));
            results.addAll(searchPhraseInField(terms, bodyIndex));

            return results.stream()
                    .sorted()
                    .toList();

        } finally {
            lock.readLock().unlock();
        }
    }

    private Set<String> searchPhraseInField(
            List<String> terms,
            Map<String, Map<String, List<Integer>>> index) {

        Set<String> results = new HashSet<>();

        Map<String, List<Integer>> firstPostings = index.get(terms.get(0));

        if (firstPostings == null) {
            return results;
        }

        for (Map.Entry<String, List<Integer>> entry : firstPostings.entrySet()) {

            String documentId = entry.getKey();
            List<Integer> firstPositions = entry.getValue();

            for (int startPosition : firstPositions) {
                boolean matches = true;

                for (int i = 1; i < terms.size(); i++) {
                    Map<String, List<Integer>> postings = index.get(terms.get(i));

                    if (postings == null) {
                        matches = false;
                        break;
                    }

                    List<Integer> positions = postings.get(documentId);

                    if (positions == null
                            || !positions.contains(startPosition + i)) {
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

    // --------------------------------------------------
    // BM25 SCORING
    // --------------------------------------------------

    private double getAverageLength(Map<String, Integer> lengths) {
        if (lengths.isEmpty()) {
            return 0.0;
        }

        long totalLength = 0;

        for (int length : lengths.values()) {
            totalLength += length;
        }

        return (double) totalLength / lengths.size();
    }
    public void replaceAllDocuments(List<Document> newDocuments) {
    Objects.requireNonNull(newDocuments, "documents must not be null");

    lock.writeLock().lock();
    try {
        clear();

        for (Document document : newDocuments) {
            addDocument(document);
        }
    } finally {
        lock.writeLock().unlock();
    }
}

    private double scoreFieldTerm(
            String term,
            String documentId,
            Map<String, Map<String, List<Integer>>> index,
            Map<String, Integer> lengths,
            double averageLength) {

        Map<String, List<Integer>> postings = index.get(term);

        if (postings == null || documents.isEmpty()) {
            return 0.0;
        }

        List<Integer> positions = postings.get(documentId);

        if (positions == null || positions.isEmpty()) {
            return 0.0;
        }

        int frequency = positions.size();
        int documentCount = documents.size();
        int documentFrequency = postings.size();

        double idf = Math.log(
                1.0 + (documentCount - documentFrequency + 0.5)
                        / (documentFrequency + 0.5));

        int documentLength = lengths.getOrDefault(documentId, 0);

        double lengthNormalization = averageLength == 0.0
                ? 1.0
                : 1.0 - B + B * documentLength / averageLength;

        return idf * (frequency * (K1 + 1.0))
                / (frequency + K1 * lengthNormalization);
    }

    private double scoreDocument(
            String term,
            String documentId,
            double averageTitleLength,
            double averageBodyLength) {

        double titleScore = scoreFieldTerm(
                term,
                documentId,
                titleIndex,
                titleLengths,
                averageTitleLength);

        double bodyScore = scoreFieldTerm(
                term,
                documentId,
                bodyIndex,
                bodyLengths,
                averageBodyLength);

        return titleBoost * titleScore + bodyScore;
    }

    // --------------------------------------------------
    // RANKED SEARCH
    // --------------------------------------------------

    public List<String> searchRanked(String query) {
        lock.readLock().lock();

        try {
            if (query == null || query.isBlank() || documents.isEmpty()) {
                return List.of();
            }

            List<String> terms = TextNormalizer.normalizeQuery(query);

            if (terms.isEmpty()) {
                return List.of();
            }

            // A document is a candidate if it matches either field.
            Set<String> candidates = new HashSet<>();

            for (String term : terms) {
                if (titleBoost > 0.0) {
                    addPostingDocuments(titleIndex, term, candidates);
                }
                addPostingDocuments(bodyIndex, term, candidates);
            }

            if (candidates.isEmpty()) {
                return List.of();
            }

            double averageTitleLength = getAverageLength(titleLengths);
            double averageBodyLength = getAverageLength(bodyLengths);

            Map<String, Double> scores = new HashMap<>();

            for (String documentId : candidates) {
                if (!documents.containsKey(documentId)) {
                    continue;
                }

                double totalScore = 0.0;

                for (String term : terms) {
                    totalScore += scoreDocument(
                            term,
                            documentId,
                            averageTitleLength,
                            averageBodyLength);
                }

                if (totalScore > 0.0) {
                    scores.put(documentId, totalScore);
                }
            }

            return scores.entrySet()
                    .stream()
                    .sorted((first, second) -> {
                        int comparison = Double.compare(
                                second.getValue(),
                                first.getValue());

                        return comparison != 0
                                ? comparison
                                : first.getKey().compareTo(second.getKey());
                    })
                    .map(Map.Entry::getKey)
                    .toList();

        } finally {
            lock.readLock().unlock();
        }
    }
}
