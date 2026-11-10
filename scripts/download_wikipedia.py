
import json
import time
import random
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path
from email.utils import parsedate_to_datetime
from datetime import datetime, timezone

API_URL = "https://en.wikipedia.org/w/api.php"

BASE_DIR = Path("datasets/wikipedia/large")
ARTICLES_DIR = BASE_DIR / "articles"
METADATA_FILE = BASE_DIR / "metadata.json"

TARGET_ARTICLES = 1000
BATCH_SIZE = 10
REQUEST_DELAY = 5
MAX_RETRIES = 8

# Replace this with a real contact email or project URL.
USER_AGENT = (
    "KestrelSearchEngine/1.0 "
    "(educational search-engine project; contact: YOUR_EMAIL_OR_PROJECT_URL)"
)

CATEGORIES = [
    "Category:Computer science",
    "Category:Programming languages",
    "Category:Information retrieval",
    "Category:Machine learning",
    "Category:Databases",
    "Category:Software engineering",
    "Category:Algorithms",
    "Category:Computer networks",
]

ARTICLES_DIR.mkdir(parents=True, exist_ok=True)

last_request_time = 0


def wait_before_request():
    global last_request_time

    elapsed = time.monotonic() - last_request_time
    if elapsed < REQUEST_DELAY:
        time.sleep(REQUEST_DELAY - elapsed)

    last_request_time = time.monotonic()


def api_request(params):
    """Make a Wikimedia API request with retry and backoff."""

    for attempt in range(MAX_RETRIES):
        wait_before_request()

        url = API_URL + "?" + urllib.parse.urlencode(params)
        request = urllib.request.Request(
            url,
            headers={"User-Agent": USER_AGENT}
        )

        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.loads(response.read().decode("utf-8"))

        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 502, 503, 504):
                raise

            retry_after = error.headers.get("Retry-After")
            delay = None

            if retry_after:
                try:
                    delay = float(retry_after)
                except ValueError:
                    try:
                        retry_date = parsedate_to_datetime(retry_after)
                        delay = max(
                            0,
                            (retry_date - datetime.now(timezone.utc)).total_seconds()
                        )
                    except (TypeError, ValueError):
                        delay = None

            if delay is None:
                delay = min(300, 10 * (2 ** attempt))

            delay += random.uniform(0, 2)

            print(
                f"HTTP {error.code}. "
                f"Retrying in {delay:.1f} seconds "
                f"(attempt {attempt + 1}/{MAX_RETRIES})..."
            )
            time.sleep(delay)

        except (urllib.error.URLError, TimeoutError) as error:
            delay = min(300, 10 * (2 ** attempt))
            print(f"Network error: {error}. Retrying in {delay} seconds...")
            time.sleep(delay)

    raise RuntimeError("Maximum API retries exceeded.")


def load_existing_metadata():
    if not METADATA_FILE.exists():
        return []

    with open(METADATA_FILE, "r", encoding="utf-8") as file:
        return json.load(file)


def save_metadata(metadata):
    METADATA_FILE.parent.mkdir(parents=True, exist_ok=True)

    temporary_file = METADATA_FILE.with_suffix(".tmp")

    with open(temporary_file, "w", encoding="utf-8") as file:
        json.dump(metadata, file, indent=2, ensure_ascii=False)

    temporary_file.replace(METADATA_FILE)


def get_valid_existing_articles(metadata):
    """Keep only metadata entries whose article files actually exist."""

    valid = []

    for article in metadata:
        filename = article.get("filename")
        if not filename or Path(filename).name != filename:
            continue

        article_path = ARTICLES_DIR / filename

        if article_path.is_file() and article_path.stat().st_size > 0:
            valid.append(article)

    return valid


def collect_article_titles():
    """Collect unique main-namespace articles from the selected categories."""

    titles = {}

    for category in CATEGORIES:
        print(f"\nCollecting from {category}")

        continuation = {}

        while len(titles) < TARGET_ARTICLES:
            params = {
                "action": "query",
                "list": "categorymembers",
                "cmtitle": category,
                "cmtype": "page",
                "cmnamespace": 0,
                "cmlimit": 50,
                "format": "json",
            }

            params.update(continuation)

            try:
                data = api_request(params)
            except Exception as error:
                print(f"Could not finish category {category}: {error}")
                break

            members = data.get("query", {}).get("categorymembers", [])

            for page in members:
                titles[page["pageid"]] = page["title"]

                if len(titles) >= TARGET_ARTICLES:
                    break

            continuation = data.get("continue", {})

            if not continuation:
                break

        print(f"Unique articles collected so far: {len(titles)}")

    return titles


def download_article_batch(batch):
    """Download extracts for one batch of pages."""

    params = {
        "action": "query",
        "pageids": "|".join(str(page_id) for page_id, _ in batch),
        "prop": "extracts",
        "explaintext": 1,
        "exsectionformat": "plain",
        "format": "json",
    }

    data = api_request(params)
    pages = data.get("query", {}).get("pages", [])

    if isinstance(pages, dict):
        pages = list(pages.values())

    return pages


def main():
    metadata = load_existing_metadata()
    metadata = get_valid_existing_articles(metadata)

    # Save the reconciled metadata before downloading anything.
    save_metadata(metadata)

    downloaded_ids = {
        int(article["page_id"])
        for article in metadata
        if "page_id" in article
    }

    print(f"Existing valid articles: {len(downloaded_ids)}")

    if len(downloaded_ids) >= TARGET_ARTICLES:
        print("Target already reached. Nothing to download.")
        return

    titles = collect_article_titles()

    remaining = [
        (page_id, title)
        for page_id, title in titles.items()
        if page_id not in downloaded_ids
    ]

    print(f"\nArticles available: {len(titles)}")
    print(f"Already downloaded: {len(downloaded_ids)}")
    print(f"Remaining to download: {len(remaining)}")

    total = len(downloaded_ids)

    for start in range(0, len(remaining), BATCH_SIZE):
        batch = remaining[start:start + BATCH_SIZE]

        try:
            pages = download_article_batch(batch)

            for page in pages:
                page_id = int(page["pageid"])
                title = page.get("title", "")
                content = page.get("extract", "").strip()

                if not content:
                    print(f"Skipping empty article: {title}")
                    continue

                filename = f"wiki-{page_id}.txt"
                article_path = ARTICLES_DIR / filename

                article_path.write_text(content, encoding="utf-8")

                metadata.append({
                    "id": f"wiki-{page_id}",
                    "title": title,
                    "page_id": page_id,
                    "url": f"https://en.wikipedia.org/?curid={page_id}",
                    "filename": filename,
                    "license": (
                        "Wikipedia content; generally available under "
                        "CC BY-SA, subject to applicable article notices."
                    ),
                })

                downloaded_ids.add(page_id)
                total += 1

            # Persist after every successful batch.
            save_metadata(metadata)

            print(f"Downloaded: {total}/{len(titles)}")

        except Exception as error:
            print(f"Batch failed: {error}")
            print("Existing downloads are preserved. You can rerun the script.")

    print("\nDownload run finished.")
    print(f"Articles downloaded and retained: {len(downloaded_ids)}")
    print(f"Output directory: {BASE_DIR.resolve()}")

    if len(downloaded_ids) < TARGET_ARTICLES:
        print(
            "Target not reached. Run the script again later "
            "to resume downloading."
        )


if __name__ == "__main__":
    main()
