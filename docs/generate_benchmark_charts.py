"""Render the recorded README figures. This does not execute a benchmark.

Requires matplotlib. Run from any directory; --preview-dir optionally writes PNGs.
Reference: https://castorini.github.io/pyserini/2cr/beir.html (2026-11-11).
"""
import argparse
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

PAGE, TEXT, MUTED, GRID = "#19181f", "#f2eef8", "#c0b6cd", "#413b4b"
KESTREL, REFERENCE = "#bda0ef", "#82bcbc"
OUTPUT = Path(__file__).resolve().parent / "assets"


def canvas(title, subtitle):
    fig, ax = plt.subplots(figsize=(10, 6), facecolor=PAGE)
    ax.set_facecolor(PAGE)
    fig.subplots_adjust(left=.13, right=.91, bottom=.24, top=.73)
    fig.text(.065, .935, title, color=TEXT, fontsize=21, weight="bold")
    fig.text(.065, .88, subtitle, color=MUTED, fontsize=11)
    ax.tick_params(colors=MUTED, labelsize=12, length=0, pad=10)
    for spine in ax.spines.values():
        spine.set_visible(False)
    ax.set_axisbelow(True)
    return fig, ax


def save(fig, name, preview):
    fig.savefig(OUTPUT / f"{name}.svg", facecolor=PAGE, metadata={"Date": "2026-11-11"})
    if preview:
        preview.mkdir(parents=True, exist_ok=True)
        fig.savefig(preview / f"{name}.png", dpi=150, facecolor=PAGE)
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview-dir", type=Path)
    args = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    fig, ax = canvas("Retrieval quality, in context", "BEIR test datasets · NDCG@10 · higher is better · shared 0–1 scale")
    names = ["SciDocs", "SciFact", "NFCorpus", "FiQA"]
    own = [.1614, .6291, .3186, .2528]
    reference = [.158, .665, .325, .236]
    y = np.arange(4)
    ax.barh(y-.17, own, height=.26, color=KESTREL, label="Kestrel · local")
    ax.barh(y+.17, reference, height=.26, color=REFERENCE, label="Anserini BM25 multifield · published")
    for i, (a, b) in enumerate(zip(own, reference)):
        ax.text(a+.012, i-.17, f"{a:.4f}", va="center", color=TEXT, fontsize=11)
        ax.text(b+.012, i+.17, f"{b:.3f}", va="center", color=TEXT, fontsize=11)
    ax.set_yticks(y, names)
    ax.invert_yaxis()
    ax.set_xlim(0, 1)
    ax.set_xticks([0, .25, .5, .75, 1])
    ax.grid(axis="x", color=GRID, linewidth=.7)
    legend = ax.legend(loc="lower left", bbox_to_anchor=(-.01, 1.06), frameon=False, ncol=2, fontsize=10)
    for text in legend.get_texts():
        text.set_color(TEXT)
    fig.text(.065, .105, "Reference: Pyserini BEIR reproductions, BM25 MF · checked November 11, 2026", color=MUTED, fontsize=10)
    fig.text(.065, .055, "Separate runs and ranking settings; contextual comparison, not a controlled head-to-head test.", color=MUTED, fontsize=10)
    save(fig, "retrieval-comparison", args.preview_dir)

    fig, ax = canvas("Concurrent search throughput", "Kestrel local SciDocs experiment · 200 warm-up queries · 400 searches per configuration")
    threads, qps = [1, 2, 4, 8], [67.63, 109.36, 215.33, 350.58]
    ax.plot(threads, qps, color=KESTREL, marker="o", linewidth=2.5, markersize=8)
    ax.fill_between(threads, qps, alpha=.08, color=KESTREL)
    for x, value in zip(threads, qps):
        ax.annotate(f"{value:.2f}", (x, value), xytext=(0, 13), textcoords="offset points", ha="center", color=TEXT, fontsize=12)
    ax.set_xticks(threads)
    ax.set_xlim(.5, 8.5)
    ax.set_ylim(0, 410)
    ax.set_yticks([0, 100, 200, 300, 400])
    ax.grid(axis="y", color=GRID, linewidth=.7)
    ax.set_xlabel("Concurrent threads", color=MUTED, fontsize=12, labelpad=12)
    ax.set_ylabel("Queries / second", color=MUTED, fontsize=12, labelpad=12)
    fig.text(.065, .105, "8 threads: 350.58 QPS · 5.18× the 1-thread baseline · P95 latency: 46.80 ms", color=TEXT, fontsize=11)
    fig.text(.065, .055, "Recorded local engine timings; excludes HTTP and database latency. Not hosted demo performance.", color=MUTED, fontsize=10)
    save(fig, "concurrent-throughput", args.preview_dir)


if __name__ == "__main__":
    main()
