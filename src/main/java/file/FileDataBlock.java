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
package file;

import conf.Configuration;

import java.io.IOException;

/**
 * It represent a disk block. The default size is 8K.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataBlock {
    private final Long ID;
    private byte[] data;

    /**
     *  FileDataBlock constructor
     *
     * @param offset
     * @param data
     */
    public FileDataBlock(final Long blockId, final byte[] data, final Integer offset,
                         final Integer length) throws IOException {
        if(data == null || data.length == 0) {
            throw new IOException("invalid data");
        }
        if(offset >= data.length) {
            throw new IOException("invalid offset [" + offset + "]");
        }
        if(length == 0 || (offset + length) > data.length) {
            throw new IOException("invalid length [" + length + "]");
        }
        this.ID = blockId;
        this.data = new byte[length];

        /*
         * Copy 8K bytes into the data array
         */
        System.arraycopy(data, offset, this.data, 0, length);
    }

    public Long getID() {
        return this.ID;
    }

    public static Integer getBlockPosition(Long blockID) {
        return Long.valueOf(blockID * Configuration.BLOCK_SIZE).intValue();
    }

    public byte[] getData() {
        return this.data;
    }

    public Integer getDataLength() {
        return this.data.length;
    }

    public void writeAt(byte[] data, Integer srcByteOffset, Integer dstByteOffset, Integer length) throws IOException {
        if(srcByteOffset > data.length || srcByteOffset < 0) {
            throw new IOException("invalid source byte offset");
        }
        if(dstByteOffset > this.data.length || dstByteOffset < 0) {
            throw new IOException("invalid byte offset");
        }
        if(length > Configuration.BLOCK_SIZE - dstByteOffset || length < 0) {
            throw new IOException("invalid byte length");
        }

        Integer bytesExceed = length - (this.data.length - dstByteOffset);
        if(bytesExceed > 0 && bytesExceed < Configuration.BLOCK_SIZE) {
            extendDataSize(bytesExceed);
        }
        System.arraycopy(data, srcByteOffset, this.data, dstByteOffset, length);
    }

    private void extendDataSize(Integer size) throws IOException {
        byte[] newData = new byte[this.data.length + size];
        System.arraycopy(this.data, 0, newData, 0, this.data.length);
        this.data = newData;
    }
}
