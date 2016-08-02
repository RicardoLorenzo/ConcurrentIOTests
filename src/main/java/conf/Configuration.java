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
package conf;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Holds many different configuration and common objects.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class Configuration {
    public static final Integer FILE_DATA_BLOCK_WRITER_THREADS = 4;
    public static final Long FILE_DATA_MAX_SIZE = 2147483648L;
    public static final Integer BLOCK_SIZE = 8192;
    public static final String FILENAME_DATA_PREFIX = "data_";
    public static final String FILENAME_DATA_SUFFIX = ".dat";
    public static final String FILENAME_INDEX_SUFFIX = ".idx";
    public static Map<String, ReadWriteLock> fileDataBlockLocks;
    public static Map<String, ReadWriteLock> fileIndexLocks;

    static {
        fileDataBlockLocks = new HashMap<String, ReadWriteLock>();
        fileIndexLocks = new HashMap<String, ReadWriteLock>();
    }

    /**
     * Gets any resource from the resource path
     * @param name
     * @return
     */
    public static File getResource(String name) {
        URL resource = Configuration.class.getClassLoader().getResource(name);
        if(resource != null && resource.getProtocol().equals("file")) {
            try {
                return new File(resource.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getResourcePath() {
        return Configuration.getResource(".").getAbsolutePath();
    }

    /**
     * Returns a unique static ReadWrite lock per file
     *
     * @param filename
     * @return
     */
    public static ReadWriteLock getFileDataBlockLock(String filename) {
        if(fileDataBlockLocks.containsKey(filename)) {
            return fileDataBlockLocks.get(filename);
        }
        ReadWriteLock lock = new ReentrantReadWriteLock();
        fileDataBlockLocks.put(filename, lock);
        return lock;
    }

    /**
     * Returns a unique static ReadWrite lock per index filename.
     *
     * @param filename
     * @return
     */
    public static ReadWriteLock getFileIndexLock(String filename) {
        if(fileIndexLocks.containsKey(filename)) {
            return fileIndexLocks.get(filename);
        }
        ReadWriteLock lock = new ReentrantReadWriteLock();
        fileIndexLocks.put(filename, lock);
        return lock;
    }
}
