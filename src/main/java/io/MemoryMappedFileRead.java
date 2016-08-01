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

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.Map;

/**
 * It can read a file by memory mapping a file invoking the
 * <a href="http://man7.org/linux/man-pages/man2/mmap.2.html">mmap system call<a/>.
 * Memory pages are initially not loaded into the resident memory or the OS page cache. They are
 * loaded as soon as the file start to be read by the page fault mechanism.
 *
 * When no resident memory is available or the OS require to free some memory, pages can be evicted
 * by selecting them using the LRU (last recent use) mechanism.
 *
 * You can verify the resident memory size within the /proc/[pid]/smaps file.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class MemoryMappedFileRead {
    private MappedByteBuffer buffer;

    public MemoryMappedFileRead(File f) throws IOException {
        FileChannel channel = new RandomAccessFile(f, "r").getChannel();
        this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    }

    /**
     * Load the data into a byte buffer.
     *
     * @param buffer
     * @return
     * @throws IOException
     */
    public Integer read(byte[] buffer) throws IOException {
        this.buffer.get(buffer);
        return this.buffer.remaining();
    }

    /**
     * Read the next line from the file, startin from the specified position. It returns an entry with the last
     * position and the line.
     *
     * @param offset
     * @return Map.Entry<Integer, String>
     * @throws IOException
     */
    public Map.Entry<Integer, String> readLine(Integer offset) throws IOException {
        StringBuilder _sb = new StringBuilder();
        if(offset != 0) {
            /**
             * Seek for the start of the next line
             */
            offset = seekEOL(offset);
        }
        for(;offset < buffer.limit() - 1; offset++) {
            Byte b = buffer.get(offset);
            /**
             * At the line break, increment the offset
             * and return the line
             */
            if(b.intValue() == 10) {
                return new AbstractMap.SimpleEntry<Integer, String>(offset, _sb.toString());
            }
            _sb.append((char) b.byteValue());
        }
        return new AbstractMap.SimpleEntry<Integer, String>(offset, _sb.length() == 0 ? null : _sb.toString());
    }

    /**
     * It seeks the next EOL and returns the position.
     *
     * @param p
     * @return
     */
    public Integer seekEOL(Integer p) {
        for(;p < buffer.limit() - 1; p++) {
            Byte b = buffer.get(p);
            /**
             * At the line break, increment the offset
             */
            if(b.intValue() == 10) {
                p++;
                return p;
            }
        }
        return p;
    }

    public Integer getOffsetLimit() {
        return this.buffer.limit();
    }
}