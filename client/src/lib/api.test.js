import { describe, expect, it, vi, afterEach } from "vitest";
import { parseResults, safeExternalUrl, search } from "./api";
vi.hoisted(() =>
  vi.stubEnv("VITE_KESTREL_API_URL", "https://kestrel-search.onrender.com"),
);
afterEach(() => vi.unstubAllGlobals());
describe("search contract boundary", () => {
  it("accepts an empty response and optional null external URLs", () => {
    expect(parseResults([])).toEqual([]);
    expect(
      parseResults([
        { id: "a", title: "A", snippet: "", url: null, rank: 1 },
      ])[0].url,
    ).toBeNull();
  });
  it("rejects ranked-ID arrays and malformed detailed results", () => {
    for (const value of [
      ["id-only"],
      {},
      [{ id: "a", title: "A", snippet: "", url: null, rank: 0 }],
      [{ id: "a", title: "A", rank: 1 }],
    ]) {
      expect(() => parseResults(value)).toThrow("unexpected result format");
    }
  });
  it("only permits safe HTTP external links", () => {
    expect(safeExternalUrl("javascript:alert(1)")).toBeUndefined();
    expect(safeExternalUrl("data:text/html,test")).toBeUndefined();
    expect(safeExternalUrl(null)).toBeUndefined();
    expect(safeExternalUrl("https://example.com/doc")).toBe(
      "https://example.com/doc",
    );
  });
  it("encodes query text and forwards cancellation to fetch", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    const fetcher = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => [] });
    vi.stubGlobal("fetch", fetcher);
    const controller = new AbortController();
    expect(await search("java & c++", controller.signal)).toEqual([]);
    expect(fetcher).toHaveBeenCalledWith(
      expect.stringContaining("q=java%20%26%20c%2B%2B"),
      expect.objectContaining({ signal: controller.signal }),
    );
  });
  it("uses real ranked IDs and document reads when the detailed route is not deployed", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    const fetcher = vi.fn(async (url) => {
      if (url.includes("/search/results")) return { ok: false, status: 404 };
      if (url.includes("/search/ranked"))
        return { ok: true, json: async () => ["b", "a"] };
      const id = url.endsWith("/b") ? "b" : "a";
      return {
        ok: true,
        json: async () => ({
          id,
          title: `Document ${id}`,
          content: " Real\n content ",
          url: null,
        }),
      };
    });
    vi.stubGlobal("fetch", fetcher);
    const signal = new AbortController().signal;
    expect(await search("java", signal)).toEqual([
      {
        id: "b",
        title: "Document b",
        snippet: "Real content",
        url: null,
        rank: 1,
      },
      {
        id: "a",
        title: "Document a",
        snippet: "Real content",
        url: null,
        rank: 2,
      },
    ]);
    expect(fetcher).toHaveBeenCalledTimes(4);
    for (const [, options] of fetcher.mock.calls)
      expect(options.signal).toBe(signal);
  });
  it("preserves a real empty ranked result without fetching documents", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 404 })
      .mockResolvedValueOnce({ ok: true, json: async () => [] });
    vi.stubGlobal("fetch", fetcher);
    expect(await search("absent", new AbortController().signal)).toEqual([]);
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it.each([500, 502, 503, 504])(
    "explains HTTP %s server failures without falling back",
    async (status) => {
      vi.stubGlobal("window", { location: { protocol: "http:" } });
      const fetcher = vi.fn().mockResolvedValue({ ok: false, status });
      vi.stubGlobal("fetch", fetcher);
      await expect(
        search("java", new AbortController().signal),
      ).rejects.toThrow("Please give it a few minutes, then retry.");
      expect(fetcher).toHaveBeenCalledTimes(1);
    },
  );
  it("does not label a bad query response as server downtime", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: false, status: 400 }),
    );
    await expect(search("java", new AbortController().signal)).rejects.toThrow(
      "HTTP 400",
    );
  });
  it("explains service failure during compatibility document reads", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce({ ok: false, status: 404 })
        .mockResolvedValueOnce({ ok: true, json: async () => ["a"] })
        .mockResolvedValueOnce({ ok: false, status: 503 }),
    );
    await expect(search("java", new AbortController().signal)).rejects.toThrow(
      "server might be temporarily down",
    );
  });
  it("rejects a document that does not match its ranked ID", async () => {
    vi.stubGlobal("window", { location: { protocol: "http:" } });
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce({ ok: false, status: 404 })
        .mockResolvedValueOnce({ ok: true, json: async () => ["expected"] })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            id: "wrong",
            title: "Wrong",
            content: "",
            url: null,
          }),
        }),
    );
    await expect(search("java", new AbortController().signal)).rejects.toThrow(
      "unexpected document format",
    );
  });
});
