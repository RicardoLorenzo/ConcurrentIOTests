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
import java.util.Base64;

/**
 * Manages the data file format. Uses Base64 in order to avoid encoding issues (However,
 * I'm not sure how relevant it is).
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataRecordFormat {

    /**
     * Decode the on disk format and retrieves the data.
     *
     * @param name
     * @param data
     * @return
     * @throws IOException
     */
    public static byte[] encode(String name, byte[] data) throws IOException {
        /*
         * Base64 encoding is probably adding an extra overhead. However, I used it to avoid encoding errors.
         */
        Base64.Encoder encoder = Base64.getEncoder();
        ByteArrayOutputStream documentEnvelope = new ByteArrayOutputStream();

        documentEnvelope.write("===== record n:\"".getBytes());
        documentEnvelope.write(name.getBytes());
        documentEnvelope.write("\" sz:".getBytes());
        documentEnvelope.write(String.valueOf(data.length).getBytes());
        documentEnvelope.write(" =====".getBytes());
        documentEnvelope.write((byte) '\n');
        documentEnvelope.write(encoder.encode(data));
        documentEnvelope.write((byte) '\n');
        documentEnvelope.close();
        return documentEnvelope.toByteArray();
    }

    /**
     * Encodes the data into the final disk format.
     *
     * @param data
     * @return
     */
    public static byte[] decode(byte[] data) {
        Base64.Decoder decoder = Base64.getDecoder();
        Integer offset = indexOf(data, "=====\n".getBytes());
        offset += "=====\n".getBytes().length;
        byte[] base64Document = new byte[(data.length - offset) - 1];
        System.arraycopy(data, offset, base64Document, 0, base64Document.length);
        return decoder.decode(base64Document);
    }

    private static Integer indexOf(byte[] data, byte[] match) {
        for(int i = 0; i < data.length - match.length + 1; ++i) {
            boolean found = true;
            for(int j = 0; j < match.length; ++j) {
                if (data[i + j] != match[j]) {
                    found = false;
                    break;
                }
            }
            if (found)  {
                return i;
            }
        }
        return -1;
    }
}
