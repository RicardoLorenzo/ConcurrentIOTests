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
package mmap;

import conf.Configuration;
import io.MemoryMappedFileRead;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Memory map read tests.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class MemoryMappedFileReadTest extends TestCase {
    private MemoryMappedFileRead memoryMappedFileRead;

    public MemoryMappedFileReadTest() throws IOException {
        memoryMappedFileRead = new MemoryMappedFileRead(Configuration.getResource("mmapfile.txt"));
    }

    @Test
    public void testMmapRead() {
        Integer offset = memoryMappedFileRead.seekEOL(184);
        try {
            Map.Entry<Integer, String> line = memoryMappedFileRead.readLine(offset);
            assertEquals("iiiiiiiiiiiiiiiiiii", line.getValue());
        } catch (IOException e) {
            assertTrue(false);
        }
    }
}
