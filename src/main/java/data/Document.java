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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Document object. This object is the high level representation of the data.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class Document implements Serializable {
    private Long ID;
    private String name;
    private String content;

    public Document(Long id, String name) {
        this.ID = id;
        this.name = name;
    }

    public String getContent() {
        return this.content;
    }

    public Long getID() {
        return this.ID;
    }

    public String getName() {
        return this.name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     *
     * This method is contributing to the on disk document format.
     *
     * @return
     * @throws IOException
     */
    public static Document deserialize(byte[] data) throws IOException {
        Document document;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            document = Document.class.cast(ois.readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException("index class format error");
        } finally {
            bais.close();
        }
        return document;
    }

    /**
     * This method is contributing to the on disk document format.
     *
     * @return
     * @throws IOException
     */
    public static byte[] serialize(Document document) throws IOException {
        ByteArrayOutputStream documentContent = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(documentContent);
        oos.writeObject(document);
        documentContent.close();
        return documentContent.toByteArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: " + this.name);
        sb.append("\n");
        sb.append("Content: " + this.content);
        return sb.toString();
    }
}
