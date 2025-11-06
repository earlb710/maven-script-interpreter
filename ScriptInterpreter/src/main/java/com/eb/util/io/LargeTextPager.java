package com.eb.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * LargeTextPager - Read-only large-text pager with optional line-aligned pages,
 * memory-mapped IO, and async prefetch. - UTF-8 safe (won't split multi-byte at
 * page boundary). - Pages are discovered lazily; supports cursor navigation and
 * random access.
 */
public final class LargeTextPager implements AutoCloseable {

    public enum Direction {
        FORWARD, BACKWARD
    }

    // ===== Builder =====
    public static final class Builder {

        private final Path path;
        private int pageSizeBytes = 2 * 1024 * 1024; // 2 MiB
        private Charset charset = StandardCharsets.UTF_8;
        private boolean lineAligned = false;
        private boolean memoryMapped = false;
        private boolean prefetch = false;
        private int prefetchDistance = 2;
        private int cachePages = 16;

        public Builder(Path path) {
            this.path = Objects.requireNonNull(path);
        }

        public Builder pageSizeBytes(int bytes) {
            this.pageSizeBytes = Math.max(4 * 1024, bytes);
            return this;
        }

        public Builder charset(Charset cs) {
            this.charset = Objects.requireNonNull(cs);
            return this;
        }

        public Builder lineAligned(boolean v) {
            this.lineAligned = v;
            return this;
        }

        public Builder memoryMapped(boolean v) {
            this.memoryMapped = v;
            return this;
        }

        public Builder prefetch(boolean v) {
            this.prefetch = v;
            return this;
        }

        public Builder prefetchDistance(int d) {
            this.prefetchDistance = Math.max(0, d);
            return this;
        }

        public Builder cachePages(int n) {
            this.cachePages = Math.max(0, n);
            return this;
        }

        public LargeTextPager build() throws IOException {
            return new LargeTextPager(this);
        }
    }

    public static Builder open(Path path) {
        return new Builder(path);
    }

    // ===== Page DTO =====
    public static final class Page {

        public final long index;
        public final long startOffset;
        public final long endOffset;
        public final String text;

        private Page(long index, long startOffset, long endOffset, String text) {
            this.index = index;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.text = text;
        }
    }

    /**
     * One regex match.
     */
    public static final class Hit {

        public final long pageIndex;     // page where match is anchored
        public final int charStart;     // char index within page.text
        public final int charEnd;       // char end (exclusive) within page.text
        public final long byteStart;     // absolute file byte offset (UTF-8 aware)
        public final long byteEnd;       // absolute file byte end (exclusive)
        public final String preview;     // short excerpt around match

        public Hit(long pageIndex, int charStart, int charEnd,
                long byteStart, long byteEnd, String preview) {
            this.pageIndex = pageIndex;
            this.charStart = charStart;
            this.charEnd = charEnd;
            this.byteStart = byteStart;
            this.byteEnd = byteEnd;
            this.preview = preview;
        }

        @Override
        public String toString() {
            return "Hit{page=" + pageIndex + ", char=[" + charStart + "," + charEnd
                    + "], bytes=[" + byteStart + "," + byteEnd + "], preview=" + preview + "}";
        }
    }

    /**
     * Async search lifecycle callbacks.
     */
    public interface SearchListener {

        default void onProgress(long pagesScanned, long pagesTotal) {
        }

        default void onHit(Hit hit) {
        }

        default void onComplete() {
        }

        default void onError(Throwable t) {
        }
    }

    /**
     * Handle for cancellation & state.
     */
    public static final class SearchHandle {

        private volatile boolean cancelled = false;
        private final Future<?> task;

        SearchHandle(Future<?> task) {
            this.task = task;
        }

        /**
         * Cancel the search (best-effort).
         */
        public void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel(true);
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    // ===== Fields =====
    private final Path path;
    private final int pageSize;
    private final Charset charset;
    private final boolean lineAligned;
    private final boolean memoryMapped;
    private final boolean prefetchEnabled;
    private final int prefetchDistance;
    private final FileChannel channel;
    private final long fileSize;

    // Lazy page index: pageStarts[i] = start offset for page i; we append as pages are discovered.
    // pageStarts[0] = 0 always if file non-empty.
    private final List<Long> pageStarts = new ArrayList<>(1024);

    // Cache: pageIndex -> Page (LRU)
    private final Map<Long, Page> pageCache;

    // Prefetch infra
    private final ExecutorService prefetchExec;

    // Synchronization
    private final Object lock = new Object();

    private LargeTextPager(Builder b) throws IOException {
        this.path = b.path;
        this.pageSize = b.pageSizeBytes;
        this.charset = b.charset;
        this.lineAligned = b.lineAligned;
        this.memoryMapped = b.memoryMapped;
        this.prefetchEnabled = b.prefetch;
        this.prefetchDistance = b.prefetchDistance;

        this.channel = FileChannel.open(path, StandardOpenOption.READ);
        this.fileSize = channel.size();

        // Initialize first page start
        if (fileSize > 0) {
            pageStarts.add(0L);
        }

        // LRU cache
        if (b.cachePages > 0) {
            this.pageCache = new LinkedHashMap<>(b.cachePages * 2, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Page> eldest) {
                    return size() > b.cachePages;
                }
            };
        } else {
            this.pageCache = null;
        }

        // Prefetch
        if (prefetchEnabled && b.cachePages > 0 && prefetchDistance > 0) {
            this.prefetchExec = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "LargeTextPager-prefetch");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.prefetchExec = null;
        }
    }

    // ===== Public API =====
    /**
     * Total file size in bytes.
     */
    public long byteSize() {
        return fileSize;
    }

    /**
     * Returns discovered page count so far. Note: for line-aligned pages this
     * grows as you read. If you want the full count, call
     * {@link #pageCountFullScan()} once (can be O(fileSize)).
     */
    public long knownPageCount() {
        synchronized (lock) {
            // If the last discovered start equals fileSize, it means last page was empty (corner), ignore.
            return Math.max(0, pageStarts.size() - (endsAtFileSize() ? 1 : 0));
        }
    }

    /**
     * Scan to the end to discover all page boundaries and return the exact page
     * count. Potentially expensive for very large files.
     */
    public long pageCountFullScan() throws IOException {
        if (fileSize == 0) {
            return 0;
        }
        synchronized (lock) {
            // Keep discovering until EOF
            while (true) {
                long lastStart = pageStarts.get(pageStarts.size() - 1);
                if (lastStart >= fileSize) {
                    break; // fully discovered
                }
                discoverNextPageBoundary(lastStart);
            }
            // If the last page end == fileSize, page count == pageStarts.size() - 1
            return pageStarts.size() - (endsAtFileSize() ? 1 : 0);
        }
    }

    /**
     * Read a page (decoded text) by index.
     */
    public String readPage(long index) throws IOException {
        return loadPage(index).text;
    }

    /**
     * Load a Page with offsets + text.
     */
    public Page loadPage(long index) throws IOException {
        if (fileSize == 0) {
            return new Page(0, 0, 0, "");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("negative page index");
        }
        Page p;
        synchronized (lock) {
            p = tryCache(index);
            if (p != null) {
                return p;
            }

            // Ensure we have discovered boundaries up to requested index
            ensurePageStartDiscovered(index);

            long start = pageStarts.get((int) index);
            long end = ensurePageEndComputed(index, start);

            // Read bytes [start, end)
            String text = readAndDecode(start, end - start);
            p = new Page(index, start, end, text);
            putCache(p);
        }

        // Async prefetch neighbors
        prefetchNeighbors(index);

        return p;
    }

    /**
     * Cursor for next/prev iteration.
     */
    public Cursor cursor() {
        return new Cursor();
    }

    public final class Cursor {

        private long i = 0;

        public long index() {
            return i;
        }

        public String current() throws IOException {
            return readPage(i);
        }

        public String next() throws IOException {
            // If we can't discover more and already at end, return empty
            synchronized (lock) {
                if (!hasNextUnlocked()) {
                    return "";
                }
                i++;
            }
            return readPage(i);
        }

        public String prev() throws IOException {
            synchronized (lock) {
                if (i == 0) {
                    return "";
                }
                i--;
            }
            return readPage(i);
        }

        public void seek(long index) throws IOException {
            if (index < 0) {
                throw new IndexOutOfBoundsException();
            }
            readPage(index); // ensures discovery
            synchronized (lock) {
                i = index;
            }
        }

        private boolean hasNextUnlocked() throws IOException {
            long pagesKnown = knownPageCount();
            if (i + 1 < pagesKnown) {
                return true;
            }
            // Try to discover one more page
            long lastStart = pageStarts.get(pageStarts.size() - 1);
            if (lastStart >= fileSize) {
                return false;
            }
            discoverNextPageBoundary(lastStart);
            return i + 1 < knownPageCount();
        }
    }

    @Override
    public void close() throws IOException {
        if (prefetchExec != null) {
            prefetchExec.shutdownNow();
        }
        channel.close();
    }

    // ===== Internals =====
    private Page tryCache(long index) {
        if (pageCache == null) {
            return null;
        }
        return pageCache.get(index);
    }

    private void putCache(Page p) {
        if (pageCache != null) {
            pageCache.put(p.index, p);
        }
    }

    private void prefetchNeighbors(long index) {
        if (prefetchExec == null) {
            return;
        }
        for (long d = 1; d <= prefetchDistance; d++) {
            long prev = index - d;
            long next = index + d;
            if (prev >= 0) {
                prefetchExec.submit(() -> silentLoad(prev));
            }
            prefetchExec.submit(() -> silentLoad(next));
        }
    }

    private void silentLoad(long idx) {
        try {
            synchronized (lock) {
                if (pageCache != null && pageCache.containsKey(idx)) {
                    return;
                }
            }
            loadPage(idx);
        } catch (Throwable ignore) {
            // Background fetch; ignore errors
        }
    }

    private boolean endsAtFileSize() {
        return !pageStarts.isEmpty() && pageStarts.get(pageStarts.size() - 1) == fileSize;
    }

    /**
     * Ensure pageStarts has an entry for requested index.
     */
    private void ensurePageStartDiscovered(long index) throws IOException {
        // pageStarts[0] exists if file non-empty
        while (pageStarts.size() <= index) {
            long lastStart = pageStarts.get(pageStarts.size() - 1);
            if (lastStart >= fileSize) {
                break;
            }
            discoverNextPageBoundary(lastStart);
        }
        if (index >= pageStarts.size() || pageStarts.get((int) index) >= fileSize) {
            throw new IndexOutOfBoundsException("page index beyond EOF");
        }
    }

    /**
     * Compute end offset for page starting at 'start' and append next start. -
     * If lineAligned: cut at last '\n' inside the window if any, else soft-cut
     * at window end. - Always maintain UTF-8 boundary by trimming trailing
     * continuation bytes if needed.
     */
    private void discoverNextPageBoundary(long start) throws IOException {
        long windowEnd = Math.min(fileSize, start + pageSize);
        if (start >= windowEnd) {
            // Degenerate end marker to avoid infinite loop
            if (pageStarts.get(pageStarts.size() - 1) != fileSize) {
                pageStarts.add(fileSize);
            }
            return;
        }

        ByteBuffer buf = readBytes(start, windowEnd - start);
        int cut = buf.limit();

        if (lineAligned) {
            int lastLf = findLastLf(buf); // index of '\n' within [0, limit)
            if (lastLf >= 0) {
                // Prefer line boundary; include the '\n'
                cut = lastLf + 1;
            }
        }

        // UTF-8 safety: trim trailing partial multibyte (if cutting not at EOF)
        if (cut < buf.limit() && charset.equals(StandardCharsets.UTF_8)) {
            cut = trimTrailingUtf8(buf, cut);
        }

        long end = start + cut;
        if (end <= start) { // Safety: don't stall
            end = Math.min(windowEnd, start + pageSize);
        }

        // Register next page start
        pageStarts.add(end);
    }

    /**
     * Read and decode byte range as string.
     */
    private String readAndDecode(long start, long len) throws IOException {
        if (len <= 0) {
            return "";
        }
        ByteBuffer buf = readBytes(start, len);
        // Direct decode via new String is OK after UTF-8 boundary handling
        // (We didn't trim here because discoverNextPageBoundary set proper end; range is exact)
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        return new String(arr, charset);
    }

    /**
     * Read bytes via FileChannel or MemoryMap.
     */
    private ByteBuffer readBytes(long start, long len) throws IOException {
        if (len <= 0) {
            return ByteBuffer.allocate(0);
        }
        if (!memoryMapped) {
            ByteBuffer buf = ByteBuffer.allocate((int) len);
            int read = channel.read(buf, start);
            if (read < 0) {
                return ByteBuffer.allocate(0);
            }
            buf.flip();
            return buf;
        } else {
            // Map region [start, start+len)
            MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_ONLY, start, len);
            mbb.load(); // hint
            return mbb; // we rely on GC to unmap eventually
        }
    }

    /**
     * Find last '\n' (0x0A) in buffer; respects current limit.
     */
    private static int findLastLf(ByteBuffer buf) {
        for (int i = buf.limit() - 1; i >= 0; i--) {
            if ((buf.get(i) & 0xFF) == 0x0A) {
                // Optionally handle CRLF by trimming preceding '\r'
                return i;
            }
        }
        return -1;
    }

    /**
     * Trim trailing partial UTF-8 sequence so we end on a code point boundary.
     * Returns a new 'cut' (<= original cut).
     */
    private static int trimTrailingUtf8(ByteBuffer buf, int cut) {
        // Look back up to 3 continuation bytes after 'cut'
        int i = cut - 1;
        int back = 0;
        while (i >= 0 && back < 3) {
            int b = buf.get(i) & 0xFF;
            if ((b & 0b1100_0000) == 0b1000_0000) { // continuation
                back++;
                i--;
            } else {
                break; // reached a start byte (or ASCII)
            }
        }
        // If we encountered continuation bytes, verify the start byte isn't beyond the cut.
        // Conservatively cut before the run of continuation bytes.
        return cut - back;
    }

    /**
     * Compute the end offset for the page at 'index'. Precondition: the start
     * for 'index' is already discovered. If the next page start is known, use
     * it; otherwise discover it now. Must be invoked while holding 'lock'.
     */
    private long ensurePageEndComputed(long index, long start) throws IOException {
        // If we already know the next page start, that's the end of this page.
        if (index + 1 < pageStarts.size()) {
            long end = pageStarts.get((int) (index + 1));
            // Safety clamp
            if (end < start) {
                end = fileSize;
            }
            if (end > fileSize) {
                end = fileSize;
            }
            return end;
        }

        // We need to discover the next boundary (line-aligned/UTF-8 safe logic lives there).
        discoverNextPageBoundary(start);

        // After discovery, the next start must exist.
        long end = pageStarts.get((int) (index + 1));
        if (end < start) {
            end = fileSize;
        }
        if (end > fileSize) {
            end = fileSize;
        }
        return end;
    }

    /**
     * Asynchronous regex search across pages.
     *
     * @param pattern compiled Pattern (use Pattern flags as desired)
     * @param startPage page index to start scanning (0..N-1)
     * @param direction FORWARD (natural) or BACKWARD (returns the last hit <= *
     * start)
     * @return SearchHandle
     * @pa
     * ra * m maxHits limit the number of hits to deliver (<=0 = unlimited)
     * @param overl apChars number of characters to carry from previous page to
     * catch boundary-spanning matches (e.g. 1024) @param listener callback for
     * hits/progress/completion @return a SearchHandle for cancellation
     */
    public SearchHandle searchAsync(
            Pattern pattern,
            long startPage,
            Direction direction,
            int maxHits,
            int overlapChars,
            SearchListener listener
    ) {

        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(listener, "listener");

        // Use the existing prefetch executor if available; otherwise create a one-off single thread
        ExecutorService exec = (this.prefetchExec != null)
                ? this.prefetchExec
                : Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "LargeTextPager-search");
                    t.setDaemon(true);
                    return t;
                });

        Callable<Void> job = () -> {
            try {
                if (fileSize == 0) {
                    listener.onComplete();
                    return null;
                }

                // Ensure starting page is discovered
                loadPage(Math.max(0, startPage));

                final long totalPages = pageCountFullScan(); // discover everything so progress is meaningful

                // FORWARD scan does natural delivery; BACKWARD will gather hits then emit last→first up to maxHits.
                final java.util.regex.Matcher matcher = pattern.matcher("");

                // Dedup key set: pageIndex:charStart:charEnd
                final Set<String> seen = new HashSet<>();

                final List<Hit> bufferedBackward = (direction == Direction.BACKWARD) ? new ArrayList<>() : null;

                long hitsDelivered = 0;

                // Scan order
                long begin = Math.max(0, Math.min(startPage, totalPages == 0 ? 0 : totalPages - 1));
                long from = (direction == Direction.FORWARD) ? begin : 0;
                long to = (direction == Direction.FORWARD) ? (totalPages - 1) : begin;

                // For forward: i = from..to; for backward: i = to..from but we’ll buffer then emit reversed around start
                long pagesScanned = 0;

                // Keep previous page for overlap calculations
                Page prev = null;

                if (direction == Direction.FORWARD) {
                    for (long i = from; i <= to; i++) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        // read current page
                        Page cur = loadPage(i);

                        // Build search text: carry tail of prev + current page
                        String carry = (prev == null) ? ""
                                : tail(prev.text, overlapChars);
                        String searchText = carry + cur.text;

                        // Run matcher
                        matcher.reset(searchText);
                        while (matcher.find()) {
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            int mStart = matcher.start();
                            int mEnd = matcher.end();

                            // Determine which page owns the match (could be in carry)
                            boolean inCarry = (mStart < carry.length());
                            long targetPageIndex;
                            int charStartInPage;
                            int charEndInPage;

                            if (inCarry) {
                                if (prev == null) {
                                    continue; // nothing to attribute
                                }
                                targetPageIndex = cur.index - 1;
                                // The match may extend into current page; we anchor start in prev
                                int startInPrev = prev.text.length() - carry.length() + mStart;
                                int endInPrevPotential = startInPrev + (mEnd - mStart);
                                // If match extends past prev.text, clamp and compute remainder
                                if (endInPrevPotential <= prev.text.length()) {
                                    charStartInPage = startInPrev;
                                    charEndInPage = endInPrevPotential;
                                } else {
                                    // Cross-page: end spills into current; set end to prev end and ignore remainder in this hit,
                                    // or make a composite. Simpler: attribute whole match to prev and clamp end to prev.text.length().
                                    charStartInPage = startInPrev;
                                    charEndInPage = prev.text.length();
                                }
                            } else {
                                targetPageIndex = cur.index;
                                charStartInPage = mStart - carry.length();
                                charEndInPage = mEnd - carry.length();
                            }

                            if (targetPageIndex < 0) {
                                continue;
                            }

                            // Load target page (prev or cur)
                            Page owner = (targetPageIndex == cur.index) ? cur : prev;
                            if (owner == null) {
                                continue;
                            }

                            // Dedup
                            String key = owner.index + ":" + charStartInPage + ":" + charEndInPage;
                            if (!seen.add(key)) {
                                continue;
                            }

                            // Compute absolute byte offsets (UTF-8 aware)
                            long byteStart = owner.startOffset + encodeLength(owner.text, 0, charStartInPage, charset);
                            long byteEnd = owner.startOffset + encodeLength(owner.text, 0, charEndInPage, charset);

                            // Build preview
                            String preview = snippet(owner.text, charStartInPage, charEndInPage, 48);

                            Hit hit = new Hit(owner.index, charStartInPage, charEndInPage, byteStart, byteEnd, preview);

                            if (direction == Direction.FORWARD) {
                                listener.onHit(hit);
                                hitsDelivered++;
                                if (maxHits > 0 && hitsDelivered >= maxHits) {
                                    listener.onComplete();
                                    return null;
                                }
                            } else {
                                bufferedBackward.add(hit); // will emit later
                            }
                        }

                        prev = cur;
                        pagesScanned++;
                        listener.onProgress(pagesScanned, totalPages);
                    }

                    // FORWARD complete
                    listener.onComplete();
                    return null;
                } else {
                    // BACKWARD: we scanned 0..begin above; now emit hits up to maxHits nearest to startPage going backwards
                    // To reflect "backward", we invert the list and then take hits that are at or before startPage first.
                    // Note: For very large files you may prefer scanning begin..0 only; here we scanned all for simplicity.
                    Collections.reverse(bufferedBackward);

                    long delivered = 0;
                    for (Hit h : bufferedBackward) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        // Prioritize hits with pageIndex <= startPage
                        if (h.pageIndex <= startPage) {
                            listener.onHit(h);
                            delivered++;
                            if (maxHits > 0 && delivered >= maxHits) {
                                listener.onComplete();
                                return null;
                            }
                        }
                    }
                    // If still room, emit the rest
                    for (Hit h : bufferedBackward) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        if (h.pageIndex > startPage) {
                            listener.onHit(h);
                            delivered++;
                            if (maxHits > 0 && delivered >= maxHits) {
                                break;
                            }
                        }
                    }
                    listener.onComplete();
                    return null;
                }

            } catch (Throwable t) {
                listener.onError(t);
                return null;
            }
        };

        Future<?> f = exec.submit(job);

        // If we created a one-off executor, shutdown when done
        if (this.prefetchExec == null) {
            f = wrapAndShutdownOnFinish(f, exec);
        }

        return new SearchHandle(f);
    }

    /**
     * Wrap the future to shutdown executor after task completes.
     */
    private static Future<?> wrapAndShutdownOnFinish(Future<?> f, ExecutorService exec) {
        // Lightweight watcher thread:
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LargeTextPager-search-watcher");
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            try {
                f.get();
            } catch (Throwable ignore) {
            }
            exec.shutdown();
        });
        return f;
    }

// Add inside LargeTextPager (helpers)
    /**
     * Encode substring to bytes with the same charset to measure byte length
     * safely.
     */
    private static int encodeLength(String s, int from, int to, Charset cs) {
        if (from < 0) {
            from = 0;
        }
        if (to < from) {
            to = from;
        }
        if (to > s.length()) {
            to = s.length();
        }
        if (from == to) {
            return 0;
        }
        return s.substring(from, to).getBytes(cs).length;
    }

    /**
     * Tail N chars (safe) of a string.
     */
    private static String tail(String s, int n) {
        if (n <= 0) {
            return "";
        }
        int len = s.length();
        if (n >= len) {
            return s;
        }
        return s.substring(len - n);
    }

    /**
     * Build a short preview snippet around [start,end).
     */
    private static String snippet(String pageText, int start, int end, int context) {
        int left = Math.max(0, start - context);
        int right = Math.min(pageText.length(), end + context);
        String prefix = (left > 0 ? "…" : "");
        String suffix = (right < pageText.length() ? "…" : "");
        return prefix + pageText.substring(left, right) + suffix;
    }
}
