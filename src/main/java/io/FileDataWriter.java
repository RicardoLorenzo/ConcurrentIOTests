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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.IntStream;

/**
 * It performs the data block writes in a data file. This class ensures the filesystem block alignment in order
 * to maximize the disk performance.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataWriter {
    private static ExecutorService writeExecutor;
    private ReadWriteLock lock;
    private final File file;

    static {
        /*
         * A static thread pool to perform the data write operations.
         */
        writeExecutor = Executors.newFixedThreadPool(Configuration.FILE_DATA_BLOCK_WRITER_THREADS);
    }

    /**
     * FileDataWriter constructor
     *
     * @param filename
     * @throws IOException
     */
    public FileDataWriter(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Configuration.getResourcePath());
        sb.append(File.separator);
        sb.append(filename);
        this.file = new File(sb.toString());

        this.lock = Configuration.getFileDataBlockLock(filename);
        if(!this.file.exists()) {
            /**
             * In this case the block alignment isn't required
             */
            this.file.createNewFile();
        }
    }

    /**
     * Writes a set of blocks in a file using the specific write threads.
     *
     * @param dataBlocks
     * @return
     * @throws IOException
     */
    public Boolean writeDataBlocks(List<FileDataBlock> dataBlocks) throws IOException {
        Integer dataBlocksPerThread = 1;
        if(dataBlocks.size() > Configuration.FILE_DATA_BLOCK_WRITER_THREADS) {
            Double.valueOf(Math.ceil(dataBlocks.size() /
                    Configuration.FILE_DATA_BLOCK_WRITER_THREADS)).intValue();
        }
        RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
        List<Callable<Boolean>> tasks = new ArrayList<>();

        IntStream.range(0, Configuration.FILE_DATA_BLOCK_WRITER_THREADS).forEach((n) -> {
            Integer offset = n * dataBlocksPerThread;
            List<FileDataBlock> threadDataBlocks = new ArrayList<>();
            IntStream.range(offset, offset + dataBlocksPerThread).forEach((position) -> {
                if(position < dataBlocks.size()) {
                    threadDataBlocks.add(dataBlocks.get(position));
                }
            });
            tasks.add(writeDataBlockTask(raf, this.lock, threadDataBlocks));
        });

        final Boolean[] success = { true };
        try {
            writeExecutor.invokeAll(tasks)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .forEach(result -> {
                        if(!result) {
                            success[0] = false;
                        }
                    });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            raf.close();
        }
        return success[0];
    }

    private static Callable writeDataBlockTask(final RandomAccessFile raf, final ReadWriteLock lock,
                                               final List<FileDataBlock> dataBlocks) {
        Callable<Boolean> task = () -> {
            /**
             * Getting the write lock.
             */
            Lock l = lock.writeLock();
            for(FileDataBlock dataBlock : dataBlocks) {
                try {
                    /*
                     * Acquiring the write lock
                     */
                    if(!l.tryLock(1, TimeUnit.SECONDS)) {
                        throw new IOException("cannot acquire a write lock");
                    }
                    Integer position = FileDataBlock.getBlockPosition(dataBlock.getID());

                    /*
                     * Seeking to the position is relevant for the block alignment. This ensures a decent
                     * disk performance.
                     *
                     * The seeking time has a performance penalty for spinning disks. However, this application is
                     * accessing randomly the disk anyway.
                     */
                    raf.seek(position);
                    raf.write(dataBlock.getData());
                } finally {
                    /*
                     * Releasing the write lock
                     */
                    l.unlock();
                }
            }
            return true;
        };
        return task;
    }

    /**
     * Stops the write threads.
     */
    public static void shutdownWriteThreads() {
        writeExecutor.shutdown();
    }
}
