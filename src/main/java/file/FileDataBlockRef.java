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

import java.io.Serializable;

/**
 * Data block reference for the index.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataBlockRef implements Serializable {
    private final Long initialBlockId;
    private final String filename;
    private final Integer byteOffset;
    private final Integer byteLength;

    public FileDataBlockRef(final String filename, final Long initialBlockId, final Integer byteOffset,
                            final Integer byteLength) {
        this.filename = filename;
        this.initialBlockId = initialBlockId;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
    }

    public Long getInitialBlockId() {
        return this.initialBlockId;
    }

    public String getFilename() {
        return this.filename;
    }

    public Integer getByteOffset() {
        return this.byteOffset;
    }

    public Integer getByteLength() {
        return this.byteLength;
    }

    /**
     * Calculates the number of blocks based on the starting position and the length
     *
     * @return
     */
    public Integer numberOfBlocks() {
        Double relativeLength = this.byteLength.doubleValue() - (Configuration.BLOCK_SIZE - this.byteOffset);
        if(relativeLength > 0) {
            return Double.valueOf(Math.ceil(relativeLength / Configuration.BLOCK_SIZE)).intValue();
        }
        return 1;
    }
}
