import { describe, expect, it } from "vitest";
import { resolveDevApiTarget } from "./backend.js";
describe("development backend configuration", () => {
  it("uses PORT ahead of the production API origin", () => {
    expect(
      resolveDevApiTarget({
        PORT: "http://localhost:8080/",
        VITE_KESTREL_API_URL: "https://kestrel-search.onrender.com",
      }),
    ).toBe("http://localhost:8080");
  });
  it("accepts a numeric backend port", () => {
    expect(resolveDevApiTarget({ PORT: "9090" })).toBe("http://localhost:9090");
  });
  it("uses the API origin when PORT is absent", () => {
    expect(
      resolveDevApiTarget({
        VITE_KESTREL_API_URL: "https://kestrel-search.onrender.com/",
      }),
    ).toBe("https://kestrel-search.onrender.com");
  });
  it.each([
    "0",
    "65536",
    "not-a-port",
    "file:///etc",
    "http://user:password@localhost:8080",
    "http://localhost:8080/api",
  ])("rejects invalid or inappropriate targets: %s", (PORT) => {
    expect(() => resolveDevApiTarget({ PORT })).toThrow();
  });
});
