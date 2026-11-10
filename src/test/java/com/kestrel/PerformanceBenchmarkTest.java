package com.kestrel;

import com.kestrel.datasets.SciDocsDatasetLoader;
import com.kestrel.datasets.SciDocsEvaluator;
import com.kestrel.datasets.SciDocsQuery;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Opt-in, single-threaded performance run. Invoke with -Dkestrel.performanceBenchmark=true. */
class PerformanceBenchmarkTest {
    private static final int[] INDEX_SIZES = {1_000, 10_000, 25_000};
    private static final int REPS = 3;
    private static final int CRUD_OPS = 500;
    private final List<String> raw = new ArrayList<>();

    record IndexingSample(int size, int repetition, long elapsedNs, long retainedBytes) {}
    record SearchSummary(int size, List<Double> latenciesMs, List<Double> qps) {}
    record Distribution(double median, double p95, double p99) {}

    @Test
    void producesReproducibleSingleThreadedReport() throws Exception {
        assumeTrue(Boolean.getBoolean("kestrel.performanceBenchmark"),
                "Run this opt-in benchmark with -Dkestrel.performanceBenchmark=true");

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();
        Path root = Path.of("datasets/scidocs");
        Path corpusPath = root.resolve("corpus.jsonl");
        List<Document> documents = loader.load(corpusPath);
        List<SciDocsQuery> queries = loader.loadQueries(root.resolve("queries.jsonl"));
        Map<String, Map<String, Integer>> qrels = loader.loadQrels(root.resolve("qrels/test.tsv"));
        assertEquals(25_657, documents.size());
        assertEquals(1_000, queries.size());

        raw.add("category,configuration,documents,repetition,operation,query_id,elapsed_ns,count,rate_per_second,heap_delta_bytes");
        printEnvironment(documents, queries, corpusPath);

        // Warm indexing at maximum measured size so the hot addDocument path reaches steady state.
        buildIndex(documents.subList(0, 25_000));
        forceGc();
        List<IndexingSample> indexingSamples = new ArrayList<>();
        for (int size : INDEX_SIZES) {
            List<Document> subset = documents.subList(0, size);
            for (int rep = 1; rep <= REPS; rep++) {
                forceGc();
                long beforeHeap = usedHeap();
                long start = System.nanoTime();
                InvertedIndex index = buildIndex(subset);
                long elapsed = System.nanoTime() - start;
                forceGc();
                long retained = Math.max(0, usedHeap() - beforeHeap);
                indexingSamples.add(new IndexingSample(size, rep, elapsed, retained));
                raw.add(String.format(Locale.ROOT, "indexing,separate-title2,%d,%d,build,,%d,%d,%.4f,%d",
                        size, rep, elapsed, size, size / (elapsed / 1e9), retained));
                System.out.printf(Locale.ROOT, "INDEX size=%d rep=%d seconds=%.4f docsPerSec=%.2f retainedHeapMiB=%.2f%n",
                        size, rep, elapsed / 1e9, size / (elapsed / 1e9), retained / 1048576.0);
                index = null;
                forceGc();
            }
        }

        Map<Integer, SearchSummary> searchSummaries = new LinkedHashMap<>();
        for (int size : INDEX_SIZES) {
            InvertedIndex index = buildIndex(documents.subList(0, size)); // construction excluded from search timing
            searchSummaries.put(size, measureSearch(size, index, queries));
        }

        // Full retrieval quality and full-corpus CRUD are measured on the production scorer only.
        long fullBuildStart = System.nanoTime();
        InvertedIndex fullIndex = buildIndex(documents);
        long fullBuildNs = System.nanoTime() - fullBuildStart;
        long evalStart = System.nanoTime();
        SciDocsEvaluator.EvaluationResult retrieval = new SciDocsEvaluator().evaluate(fullIndex, queries, qrels, 10);
        long evalNs = System.nanoTime() - evalStart;
        raw.add(String.format(Locale.ROOT, "retrieval,separate-title2,%d,1,evaluation,,%d,%d,,",
                documents.size(), evalNs, queries.size()));
        raw.add(String.format(Locale.ROOT, "build,separate-title2,%d,1,full_index,,%d,%d,%.4f,",
                documents.size(), fullBuildNs, documents.size(), documents.size() / (fullBuildNs / 1e9)));
        System.out.printf(Locale.ROOT,
                "SCIDOCS documents=%d queries=%d fullBuildSeconds=%.4f evaluationSeconds=%.3f P10=%.6f R10=%.6f NDCG10=%.6f%n",
                documents.size(), retrieval.evaluatedQueries(), fullBuildNs / 1e9, evalNs / 1e9,
                retrieval.precisionAtK(), retrieval.recallAtK(), retrieval.ndcgAtK());
        Map<String, Distribution> crud = measureCrud(fullIndex, documents);

        Files.write(Path.of("PERFORMANCE_BENCHMARK_RAW.csv"), raw);
        writeReport(documents, queries, corpusPath, indexingSamples, searchSummaries,
                retrieval, fullBuildNs, evalNs, crud);
        System.out.println("REPORT PERFORMANCE_BENCHMARK_REPORT.md");
        System.out.println("RAW PERFORMANCE_BENCHMARK_RAW.csv");
    }

    private static InvertedIndex buildIndex(List<Document> docs) {
        InvertedIndex index = new InvertedIndex(); // production separate-field BM25, title weight 2.0
        docs.forEach(index::addDocument);
        return index;
    }

    private SearchSummary measureSearch(int size, InvertedIndex index, List<SciDocsQuery> queries) {
        // Warm-up is separated from timed repetitions; every pass uses the identical ordered query list.
        for (int pass = 0; pass < 2; pass++) for (SciDocsQuery query : queries) index.searchRanked(query.text());
        List<Double> latency = new ArrayList<>();
        List<Double> qps = new ArrayList<>();
        for (int rep = 1; rep <= REPS; rep++) {
            long batchStart = System.nanoTime();
            for (SciDocsQuery query : queries) {
                long start = System.nanoTime();
                index.searchRanked(query.text());
                long elapsed = System.nanoTime() - start;
                latency.add(elapsed / 1_000_000.0);
                raw.add(String.format(Locale.ROOT, "search,separate-title2,%d,%d,query,%s,%d,1,,",
                        size, rep, query.id(), elapsed));
            }
            long batchElapsed = System.nanoTime() - batchStart;
            double rate = queries.size() / (batchElapsed / 1e9);
            qps.add(rate);
            raw.add(String.format(Locale.ROOT, "throughput,separate-title2,%d,%d,query_batch,,%d,%d,%.4f,",
                    size, rep, batchElapsed, queries.size(), rate));
        }
        Distribution d = distribution(latency);
        double qpsMedian = median(qps);
        System.out.printf(Locale.ROOT,
                "SEARCH size=%d n=%d medianMs=%.4f p95Ms=%.4f p99Ms=%.4f singleThreadQpsMedian=%.2f reps=%d%n",
                size, latency.size(), d.median(), d.p95(), d.p99(), qpsMedian, REPS);
        return new SearchSummary(size, latency, qps);
    }

    private Map<String, Distribution> measureCrud(InvertedIndex index, List<Document> corpus) {
        List<Double> insertMs = new ArrayList<>(), updateMs = new ArrayList<>(), deleteMs = new ArrayList<>();
        for (int rep = 1; rep <= REPS; rep++) {
            List<String> insertedIds = new ArrayList<>();
            for (int i = 0; i < CRUD_OPS; i++) {
                Document sample = corpus.get(i);
                String id = "perf-temp-" + rep + "-" + i;
                Document temp = new Document(id, sample.getTitle(), sample.getContent(), null);
                long start = System.nanoTime(); index.addDocument(temp); long elapsed = System.nanoTime() - start;
                insertMs.add(elapsed / 1_000_000.0); insertedIds.add(id);
                raw.add(String.format(Locale.ROOT, "crud,separate-title2,25657,%d,insert,%s,%d,1,,", rep, id, elapsed));
            }
            List<Document> originals = new ArrayList<>(CRUD_OPS);
            for (int i = 0; i < CRUD_OPS; i++) {
                Document original = corpus.get(i);
                originals.add(original);
                Document replacement = new Document(original.getId(), original.getTitle() + " updated",
                        original.getContent() + " updated benchmark token", null);
                long start = System.nanoTime(); index.addDocument(replacement); long elapsed = System.nanoTime() - start;
                updateMs.add(elapsed / 1_000_000.0);
                raw.add(String.format(Locale.ROOT, "crud,separate-title2,25657,%d,update,%s,%d,1,,", rep, original.getId(), elapsed));
            }
            for (String id : insertedIds) {
                long start = System.nanoTime(); index.removeDocument(id); long elapsed = System.nanoTime() - start;
                deleteMs.add(elapsed / 1_000_000.0);
                raw.add(String.format(Locale.ROOT, "crud,separate-title2,25657,%d,delete,%s,%d,1,,", rep, id, elapsed));
            }
            originals.forEach(index::addDocument); // restoration is outside measured update latency
        }
        Map<String, Distribution> result = new LinkedHashMap<>();
        result.put("insert", printCrud("insert", insertMs));
        result.put("update", printCrud("update", updateMs));
        result.put("delete", printCrud("delete", deleteMs));
        return result;
    }

    private Distribution printCrud(String name, List<Double> samples) {
        Distribution d = distribution(samples);
        System.out.printf(Locale.ROOT, "CRUD operation=%s n=%d medianMs=%.4f p95Ms=%.4f p99Ms=%.4f%n",
                name, samples.size(), d.median(), d.p95(), d.p99());
        return d;
    }

    private void printEnvironment(List<Document> docs, List<SciDocsQuery> queries, Path corpusPath) throws Exception {
        double titleChars = docs.stream().mapToInt(d -> d.getTitle().length()).average().orElse(0);
        double bodyChars = docs.stream().mapToInt(d -> d.getContent().length()).average().orElse(0);
        double titleTerms = docs.stream().mapToInt(d -> TextNormalizer.searchableLength(d.getTitle())).average().orElse(0);
        double bodyTerms = docs.stream().mapToInt(d -> TextNormalizer.searchableLength(d.getContent())).average().orElse(0);
        var runtime = ManagementFactory.getRuntimeMXBean();
        System.out.printf("ENV os=%s %s arch=%s cpu=AMD Ryzen 5 5600H with Radeon Graphics cores=%d physicalMemoryBytes=16473133056 java=%s vendor=%s maxHeapMiB=%.1f usedHeapMiB=%.1f vmArgs=%s mavenOpts=%s javaToolOptions=%s%n",
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(), System.getProperty("java.version"), System.getProperty("java.vendor"),
                Runtime.getRuntime().maxMemory() / 1048576.0, usedHeap() / 1048576.0, runtime.getInputArguments(),
                System.getenv().getOrDefault("MAVEN_OPTS", "<unset>"), System.getenv().getOrDefault("JAVA_TOOL_OPTIONS", "<unset>"));
        System.out.printf(Locale.ROOT,
                "CORPUS docs=%d fileBytes=%d queries=%d meanTitleChars=%.1f meanBodyChars=%.1f meanTitleSearchTerms=%.2f meanBodySearchTerms=%.2f%n",
                docs.size(), Files.size(corpusPath), queries.size(), titleChars, bodyChars, titleTerms, bodyTerms);
    }

    private void writeReport(List<Document> docs, List<SciDocsQuery> queries, Path corpusPath,
            List<IndexingSample> indexing, Map<Integer, SearchSummary> searches,
            SciDocsEvaluator.EvaluationResult retrieval, long fullBuildNs, long evalNs,
            Map<String, Distribution> crud) throws Exception {
        StringBuilder md = new StringBuilder("# Kestrel In-Memory Performance Benchmark\n\n");
        md.append("Date: 2026-11-01. Production configuration: separate-field BM25, title weight 2.0, K1=1.5, B=0.75. The benchmark is single-threaded; database and HTTP/API time are excluded.\n\n");
        md.append("## Environment\n\n");
        double avgTitleChars = docs.stream().mapToInt(d -> d.getTitle().length()).average().orElse(0);
        double avgBodyChars = docs.stream().mapToInt(d -> d.getContent().length()).average().orElse(0);
        double avgTitleTerms = docs.stream().mapToInt(d -> TextNormalizer.searchableLength(d.getTitle())).average().orElse(0);
        double avgBodyTerms = docs.stream().mapToInt(d -> TextNormalizer.searchableLength(d.getContent())).average().orElse(0);
        md.append("- Hardware: AMD Ryzen 5 5600H with Radeon Graphics; ").append(Runtime.getRuntime().availableProcessors())
                .append(" logical processors visible to JVM (host inventory: 6 cores / 12 threads); 15.34 GiB physical RAM.\n")
                .append("- OS/JVM: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version"))
                .append("; Java ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(").\n")
                .append("- Maximum heap: ").append(String.format(Locale.ROOT, "%.1f MiB", Runtime.getRuntime().maxMemory() / 1048576.0))
                .append(". JVM input arguments: `").append(ManagementFactory.getRuntimeMXBean().getInputArguments()).append("`.\n")
                .append("- `MAVEN_OPTS`: `").append(System.getenv().getOrDefault("MAVEN_OPTS", "<unset>"))
                .append("`; `JAVA_TOOL_OPTIONS`: `").append(System.getenv().getOrDefault("JAVA_TOOL_OPTIONS", "<unset>")).append("`.\n")
                .append("- Dataset: SciDocs corpus JSONL ").append(Files.size(corpusPath)).append(" bytes, ").append(docs.size())
                .append(" docs; ").append(queries.size()).append(" ordered queries. Mean title/body length is ")
                .append(String.format(Locale.ROOT, "%.1f/%.1f characters and %.2f/%.2f searchable terms", avgTitleChars, avgBodyChars, avgTitleTerms, avgBodyTerms))
                .append(". 1K/10K/25K indexing/search runs use deterministic prefixes from the same corpus. Full retrieval uses all documents.\n")
                .append("- Prefix levels are first 1,000, 10,000, and 25,000 records in the corpus file; this is a size-scaling benchmark, not a randomly sampled subset. Corpus fields remain title/body separated.\n\n");
        md.append("## Method\n\n")
                .append("- Indexing: one discarded 25K build warms class loading, normalizer, and map allocation at maximum measured size; then three fresh index builds per prefix size, with GC requested before each sample. Construction time excludes corpus JSON parsing. Throughput is documents/build-seconds.\n")
                .append("- Heap: retained-heap delta is measured after forced GC before and after build while the source corpus object list stays resident. It estimates incremental live Java heap for index structures, not process RSS or native memory. JVM GC ergonomics can affect the estimate.\n")
                .append("- Search: each size builds a separate production index; construction is outside query timing. Two full passes over the identical 1,000 ordered queries warm the index; three timed passes collect per-query latency and full-batch single-thread QPS. No parallel requests.\n")
                .append("- CRUD: on a full 25,657-document index, three repetitions of 500 insertions, 500 replacements of existing documents, and 500 deletions of the inserted documents. Original updated documents are restored outside the timed update operation. Reported values are per operation.\n")
                .append("- Retrieval: existing corrected SciDocs evaluator, all 1,000 queries/qrels, K=10; separate-field BM25 production default.\n")
                .append("- Raw sample rows are in `PERFORMANCE_BENCHMARK_RAW.csv`. The full test suite skips this opt-in run unless `-Dkestrel.performanceBenchmark=true` is supplied.\n\n");
        md.append("## Indexing and retained heap\n\n| Documents | Median build s | Median docs/s | Median retained heap MiB | Repetitions |\n|---:|---:|---:|---:|---:|\n");
        for (int size : INDEX_SIZES) {
            List<IndexingSample> samples = indexing.stream().filter(s -> s.size() == size).toList();
            md.append(String.format(Locale.ROOT, "| %d | %.3f | %.0f | %.2f | %d |%n", size,
                    median(samples.stream().map(s -> s.elapsedNs() / 1e9).toList()),
                    median(samples.stream().map(s -> size / (s.elapsedNs() / 1e9)).toList()),
                    median(samples.stream().map(s -> s.retainedBytes() / 1048576.0).toList()), samples.size()));
        }
        md.append("\n## Search latency and single-thread throughput\n\n| Documents | Timed query samples | Median ms | P95 ms | P99 ms | Median QPS |\n|---:|---:|---:|---:|---:|---:|\n");
        for (int size : INDEX_SIZES) {
            SearchSummary s = searches.get(size); Distribution d = distribution(s.latenciesMs());
            md.append(String.format(Locale.ROOT, "| %d | %d | %.3f | %.3f | %.3f | %.2f |%n", size,
                    s.latenciesMs().size(), d.median(), d.p95(), d.p99(), median(s.qps())));
        }
        md.append("\n## Full corrected SciDocs retrieval\n\n")
                .append(String.format(Locale.ROOT, "- Documents: %d; judged queries: %d; full index build: %.3f s; evaluation duration: %.3f s.\n- P@10: %.4f; R@10: %.4f; NDCG@10: %.4f.\n\n",
                        docs.size(), retrieval.evaluatedQueries(), fullBuildNs / 1e9, evalNs / 1e9,
                        retrieval.precisionAtK(), retrieval.recallAtK(), retrieval.ndcgAtK()))
                .append("## CRUD latency on full index\n\n");
        md.append("| Operation | Samples | Median ms | P95 ms | P99 ms |\n|---|---:|---:|---:|---:|\n");
        for (String operation : List.of("insert", "update", "delete")) {
            Distribution d = crud.get(operation);
            md.append(String.format(Locale.ROOT, "| %s | 1,500 | %.4f | %.4f | %.4f |%n", operation,
                    d.median(), d.p95(), d.p99()));
        }
        md.append("\nThese are direct in-memory `InvertedIndex` operations with no service, database, or HTTP/API overhead. Updated corpus documents are restored outside the timed interval.\n\n")
                .append("## Resume-safe interpretation\n\n")
                .append("Prefer the measured full-SciDocs retrieval metrics, corpus size, and the repeated median search latency/QPS for a clearly stated corpus/configuration. Indexing throughput is suitable only with the prefix-and-repetition methodology attached. Heap delta is an estimate rather than total process memory. Do not publish CRUD percentiles without the operation definition and repetitions. Avoid comparing these in-memory results with API/database latency or unrelated hardware/configurations.\n");
        Files.writeString(Path.of("PERFORMANCE_BENCHMARK_REPORT.md"), md);
    }

    private static long usedHeap() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    private static void forceGc() { System.gc(); try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private static double median(List<Double> values) { return percentile(values, .5); }
    private static Distribution distribution(List<Double> input) {
        List<Double> sorted = input.stream().sorted().toList();
        return new Distribution(percentile(sorted, .5), percentile(sorted, .95), percentile(sorted, .99));
    }
    private static double percentile(List<Double> sorted, double p) { return sorted.get(Math.max(0, (int) Math.ceil(p * sorted.size()) - 1)); }
}
