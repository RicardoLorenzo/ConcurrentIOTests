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
import file.ConcurrentFileTextSearch;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Concurrent search tests.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class ConcurrentFileTextSearchTest extends TestCase {
    private ConcurrentFileTextSearch concurrentFileTextSearch;

    public ConcurrentFileTextSearchTest() {
        concurrentFileTextSearch = new ConcurrentFileTextSearch();
    }

    @Test
    public void testSearch() {
        try {
            List<String> resultLines = concurrentFileTextSearch.search(Configuration.getResource("mmapfile.txt"),
                    "hi", 4);
            assertEquals(5, resultLines.size());
        } catch (IOException e) {
            assertTrue(false);
        }
    }
}
