/*
 * DirectoryAuthenticator class
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Author: Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
package file;

import io.MemoryMappedFileRead;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * It spawns multiple threads for examining different data regions within the file.
 *
 * Reads the file from memory by mapping it into the resident memory using
 * <a href="http://man7.org/linux/man-pages/man2/mmap.2.html">mmap system call<a/>.
 * Memory pages are initially not loaded into the resident memory or the OS page cache. They are
 * loaded as soon as the file start to be read by the page fault mechanism.
 *
 * When no resident memory is available or the OS require to free some memory, pages can be evicted
 * by selecting them using the LRU (last recent use) mechanism.
 *
 * Because different threads will access concurrently different areas of the file, I expect the
 * application will perform random reads mostly.
 *
 * Because no writes will be performed, I do not expected any dirty pages that require to be flushed to the disk.
 *
 * You can verify the resident memory size within the /proc/[pid]/smaps file.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class ConcurrentFileTextSearch {

    /**
     * Performs the search over the file by deviding the memory data regions and assign them to
     * the threads.
     *
     * @param f
     * @param match
     * @param threads
     * @throws IOException
     */
    public static List<String> search(final File f, final String match, final Integer threads) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        /**
         * Defining a readWrite lock.
         *
         * This lock is not necessary, but is here for any possible future write operations.
         * However, write operations using MMAP could end easily in data corruption as the OS controls the
         * data flush (it can be forced using <a href="http://linux.die.net/man/2/fsync">fsync system call</a>)
         * in to the disk and this could not succeed if the process is aborted abnormally.
         *
         */
        ReadWriteLock lock = new ReentrantReadWriteLock();
        MemoryMappedFileRead mMapRead = new MemoryMappedFileRead(f);
        List<Callable<List<String>>> tasks = new ArrayList<Callable<List<String>>>();

        /**
         * Defining data regions within the file for each thread
         */
        Integer threadOffsetLimit = Double.valueOf(Math.floor(mMapRead.getOffsetLimit() / (threads))).intValue();
        IntStream.range(0, threads).forEach((n) -> {
            tasks.add(searchTask(mMapRead, lock, match, Integer.valueOf(threadOffsetLimit * n),
                    Integer.valueOf(threadOffsetLimit * (n + 1))));
        });

        List<String> resultLines = new ArrayList<String>();
        try {
            executor.invokeAll(tasks)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(list -> !list.isEmpty())
                    .forEach(list -> {
                        resultLines.addAll(list);
                    });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.shutdown();

        return resultLines;
    }

    private static Callable searchTask(MemoryMappedFileRead mMapRead, ReadWriteLock lock, final String s,
                                       final Integer offset, final Integer offsetLimit) {
        Callable<List<String>> task = () -> {
            List<String> lines = new ArrayList<>();
            /**
             * Getting the read lock.
             */
            Lock l = lock.readLock();
            Map.Entry<Integer, String> line = new AbstractMap.SimpleEntry<Integer, String>(offset, null);
            while(true) {
                if(line.getKey() >= offsetLimit) {
                    break;
                }
                /*
                 * Acquiring the read lock
                 */
                if(!l.tryLock(1, TimeUnit.SECONDS)) {
                    throw new IOException("cannot acquire a read lock");
                }
                /*
                 * Reading a line from the specific offset from the MMAP buffer.
                 */
                line = mMapRead.readLine(line.getKey());
                /*
                 * Releasing the read lock
                 */
                l.unlock();
                if(line.getValue() == null) {
                    break;
                }
                if(line.getValue().contains(s)) {
                    lines.add(line.getValue());
                }
            }
            return lines;
        };
        return task;
    }
}
