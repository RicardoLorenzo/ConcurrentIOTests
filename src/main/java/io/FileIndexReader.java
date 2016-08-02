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
package io;

import conf.Configuration;
import data.DocumentIndex;

import java.io.*;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Writes the index data into the backing file.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileIndexReader {
    private ReadWriteLock lock;
    private final File file;

    /**
     * FileIndexReader constructor
     *
     * @param filename
     * @throws IOException
     */
    public FileIndexReader(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Configuration.getResourcePath());
        sb.append(File.separator);
        sb.append(filename);
        this.file = new File(sb.toString());

        this.lock = Configuration.getFileIndexLock(filename);
        if(!this.file.exists()) {
            throw new IOException("index file not found");
        }
    }

    /**
     * Read the document index from the file
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public DocumentIndex readIndex() throws IOException, InterruptedException {
        try {
            DocumentIndex index = null;
            if(this.file.length() == 0) {
                return new DocumentIndex();
            }
            /**
             * Getting the read lock.
             */
            Lock l = lock.readLock();
            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(this.file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            try {
                /*
                 * Acquiring the read lock
                 */
                if(!l.tryLock(1, TimeUnit.SECONDS)) {
                    throw new IOException("cannot acquire a read lock");
                }
                /*
                 * Read serialized bytes from the index.
                 */
                index = new DocumentIndex(TreeMap.class.cast(ois.readObject()));
            } catch (ClassNotFoundException e) {
                throw new IOException("index class format error");
            } finally {
                fis.close();
            }
            /*
             * Releasing the read lock
             */
            l.unlock();

            return index;
        } catch(FileNotFoundException e) {
            throw new IOException("index file not found");
        }
    }
}
