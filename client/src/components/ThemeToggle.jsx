import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

export default function ThemeToggle() {
  const [theme, setTheme] = useState(
    () => document.documentElement.dataset.theme || "dark",
  );
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", theme === "light" ? "#f6f4f9" : "#18181d");
    try {
      localStorage.setItem("kestrel-theme", theme);
    } catch {
      /* Theme remains usable without storage. */
    }
  }, [theme]);
  return (
    <button
      className="theme-toggle"
      role="switch"
      aria-label="Light mode"
      aria-checked={theme === "light"}
      title={theme === "light" ? "Switch to dark mode" : "Switch to light mode"}
      onClick={() => setTheme(theme === "light" ? "dark" : "light")}
    >
      <Moon size={15} />
      <Sun size={15} />
      <span className="theme-thumb" aria-hidden="true" />
    </button>
  );
}
