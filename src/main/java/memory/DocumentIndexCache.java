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
package memory;

import conf.Configuration;
import data.DocumentIndex;
import file.FileDataBlockRef;
import io.FileIndexReader;
import io.FileIndexWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the document indexes.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class DocumentIndexCache {
    private static Map<String, DocumentIndex> indexes;
    private static Map<String, Lock> indexLocks;

    static {
        indexes = new HashMap<>();
        indexLocks = new HashMap<>();
        loadIndexes();
    }

    private static void loadIndexes() {
        File directory = new File(Configuration.getResourcePath());
        Arrays.asList(directory.listFiles()).stream()
                .filter((f) -> {
                    if(f.isFile() && f.getName().endsWith(".idx")) {
                        return true;
                    }
                    return false;
                })
                .forEach((f) -> {
                    try {
                        String indexName = f.getName();
                        indexName = indexName.substring(0, indexName.length() -
                                Configuration.FILENAME_INDEX_SUFFIX.length());
                        FileIndexReader reader = new FileIndexReader(f.getName());
                        indexLocks.put(indexName, new ReentrantLock());
                        indexes.put(indexName, reader.readIndex());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Retrieves an specific index from memory.
     *
     * @param indexName
     * @return
     */
    public static DocumentIndex getIndex(String indexName) {
        if(!indexes.containsKey(indexName)) {
            DocumentIndex index = new DocumentIndex();
            indexes.put(indexName, index);
            return index;
        }
        return indexes.get(indexName);
    }

    private static Lock getIndexLock(String indexName) throws IOException, InterruptedException {
        if(!indexLocks.containsKey(indexName)) {
            indexLocks.put(indexName, new ReentrantLock());
        }
        return indexLocks.get(indexName);
    }

    /**
     * Removes a key from an index.
     *
     * @param indexName
     * @param key
     * @throws InterruptedException
     * @throws IOException
     */
    public static void removeIndexKey(String indexName, String key) throws
            InterruptedException, IOException {
        DocumentIndex index = getIndex(indexName);
        Lock lock = indexLocks.get(indexName);

        try {
            /*
             * Acquiring the lock
             */
            if (!lock.tryLock(1, TimeUnit.SECONDS)) {
                throw new IOException("cannot acquire a read lock");
            }

            index.removeDataBlockRefs(key);
            indexes.put(indexName, index);
        } finally {
            /*
             * Releasing the lock
             */
            lock.unlock();
        }
    }

    /**
     * Sets an specific index key in the memory.
     *
     * @param indexName
     * @param key
     * @param ref
     * @throws InterruptedException
     * @throws IOException
     */
    public static void setIndexKey(String indexName, String key, FileDataBlockRef ref) throws
            InterruptedException, IOException {
        DocumentIndex index = getIndex(indexName);
        Lock lock = getIndexLock(indexName);

        try {
            /*
             * Acquiring the lock
             */
            if(!lock.tryLock(1, TimeUnit.SECONDS)) {
                throw new IOException("cannot acquire a read lock");
            }

            index.setDataBlockRefs(key, ref);
            indexes.put(indexName, index);
        } finally {
                /*
                 * Releasing the lock
                 */
            lock.unlock();
        }
    }

    /**
     * It flushes the indexes to disk. It is likely that acquiring a global lock is something required here
     * in order to ensure the data consistency.
     *
     * TODO This method must be improved by using the journal mechanism
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void flushToDisk() throws IOException, InterruptedException {
        for(String indexName : indexes.keySet()) {
            StringBuilder indexFileName = new StringBuilder();
            indexFileName.append(indexName);
            indexFileName.append(Configuration.FILENAME_INDEX_SUFFIX);

            FileIndexWriter writer = new FileIndexWriter(indexFileName.toString());
            writer.writeIndex(indexes.get(indexName));
        }
    }
}
