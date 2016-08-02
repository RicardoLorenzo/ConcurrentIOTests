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
package data;

import file.FileDataBlockRef;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.TreeMap;

/**
 * Represent an index. The index uses a TreeMap object which is using a BTree like structure. Black/Red trees are
 * useful to balance the keys in the tree.
 *
 * For now, there is a single file for backing each index.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class DocumentIndex {
    private TreeMap<String, FileDataBlockRef> index;

    /**
     *  Document Index constructor.
     */
    public DocumentIndex() {
        this.index = new TreeMap<String, FileDataBlockRef>();
    }

    /**
     * Document Index constructor.
     *
     * @param map
     */
    public DocumentIndex(TreeMap map) {
        this.index = map;
    }

    /**
     * Checks if a key is present in the index.
     *
     * @param key
     * @return
     */
    public Boolean containsKey(String key) {
        return this.index.containsKey(key);
    }

    /**
     * Returns the datablock reference for the key.
     *
     * @param key
     * @return
     */
    public FileDataBlockRef getDataBlockRefs(String key) {
        return this.index.get(key);
    }

    /**
     * Remevoes a key from the index.
     *
     * @param key
     */
    public void removeDataBlockRefs(String key) {
        this.index.remove(key);
    }

    /**
     * Sets a datablock reference for a key.
     *
     * @param key
     * @param ref
     */
    public void setDataBlockRefs(String key, FileDataBlockRef ref) {
        this.index.put(key, ref);
    }

    /**
     * This method is the key for the on disk index format.
     *
     * @return
     * @throws IOException
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this.index);
        baos.close();
        return baos.toByteArray();
    }
}
