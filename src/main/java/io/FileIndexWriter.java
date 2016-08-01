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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Writes the index data into the backing file.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileIndexWriter {
    private ReadWriteLock lock;
    private final File file;

    public FileIndexWriter(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Configuration.getResourcePath());
        sb.append(File.separator);
        sb.append(filename);
        this.file = new File(sb.toString());

        this.lock = Configuration.getFileIndexLock(filename);
        if(!this.file.exists()) {
            this.file.createNewFile();
        }
    }

    /**
     * It writes the index into the backing file. It doesn't make sense using a write thread pool here. Instead
     * this method will use the current thread.
     *
     *
     * @param index
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Boolean writeIndex(DocumentIndex index) throws IOException, InterruptedException {
        try {
            RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
            /**
             * Getting the write lock.
             */
            Lock l = lock.writeLock();

            try {
                /*
                 * Acquiring the write lock
                 */
                if(!l.tryLock(1, TimeUnit.SECONDS)) {
                    throw new IOException("cannot acquire a write lock");
                }
                /*
                 * Write serialized bytes from the index.
                 */
                raf.write(index.serialize());
            } finally {
                raf.close();
            }
            /*
             * Releasing the write lock
             */
            l.unlock();

            return true;
        } catch(FileNotFoundException e) {
            throw new IOException("file not found");
        }
    }
}
