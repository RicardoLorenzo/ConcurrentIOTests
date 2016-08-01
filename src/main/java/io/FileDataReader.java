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
import file.FileDataBlock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * It performs the data block reads from a data file.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataReader {
    private ReadWriteLock lock;
    private final File file;
    private Long offset;

    public FileDataReader(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Configuration.getResourcePath());
        sb.append(File.separator);
        sb.append(filename);
        this.file = new File(sb.toString());
        if(!this.file.exists()) {
            throw new IOException("file not found");
        }
        this.lock = Configuration.getFileDataBlockLock(filename);
    }

    public Integer countDataBlocks() {
        if(this.file.length() == 0) {
            return 0;
        }
        Double numberOfBlocks = Long.valueOf(this.file.length()).doubleValue();
        numberOfBlocks /= Configuration.BLOCK_SIZE;
        return Double.valueOf(Math.ceil(numberOfBlocks)).intValue();
    }

    public List<FileDataBlock> getDataBlocks(final Long initialBlock, final Integer initialOffset,
                                             final Integer numberOfBlocks, final Integer documentLength)
            throws InterruptedException, IOException {
        List<FileDataBlock> dataBlocks = new ArrayList<>();
        if(this.file.length() == 0) {
            return dataBlocks;
        }
        RandomAccessFile raf = new RandomAccessFile(this.file, "r");

        try {
            /**
             * Getting the read lock.
             */
            Lock l = lock.readLock();

            Integer offset = 0;
            for(Integer counter = 0; counter <= numberOfBlocks; counter++) {
                try {
                    /*
                     * Acquiring the read lock
                     */
                    if (!l.tryLock(1, TimeUnit.SECONDS)) {
                        throw new IOException("cannot acquire a write lock");
                    }

                    Integer bytesToRead = Configuration.BLOCK_SIZE;
                    if(counter == 0) {
                        bytesToRead -= initialOffset;
                    }
                    if((offset + bytesToRead) > documentLength) {
                        bytesToRead -= (offset + bytesToRead) - documentLength;
                    }
                    byte[] data = new byte[bytesToRead];
                    Integer position = FileDataBlock.getBlockPosition(initialBlock + counter);
                    if(counter == 0) {
                        position += initialOffset;
                    }
                    raf.seek(position);
                    Integer bytesRead = raf.read(data);
                    if(bytesRead <= 0) {
                        break;
                    }
                    offset += bytesRead;

                    FileDataBlock fdb = new FileDataBlock(initialBlock + counter, data, 0, bytesRead);
                    dataBlocks.add(fdb);
                } finally {
                    /*
                     * Releasing the read lock
                     */
                    l.unlock();
                }
            }
        } finally {
            raf.close();
        }

        return dataBlocks;
    }

    public FileDataBlock getLastDataBlock() throws IOException, InterruptedException {
        Long count = countDataBlocks().longValue();
        if(count == 0) {
            return null;
        }
        count += - 1;
        List<FileDataBlock> blocks = getDataBlocks(count, 0, 1, Configuration.BLOCK_SIZE);
        if(!blocks.isEmpty()) {
            return blocks.get(0);
        }
        return null;
    }
}