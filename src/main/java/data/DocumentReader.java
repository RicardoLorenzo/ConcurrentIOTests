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

import file.FileDataBlock;
import file.FileDataBlockRef;
import io.FileDataRecordFormat;
import io.FileDataReader;
import memory.DocumentIndexCache;

import java.io.*;
import java.util.List;

/**
 * Reads the documents. At this time, it will read the documents from disk, but it will require a document
 * cache for increasing the performance.
 *
 * TODO a document cache will improve the performance
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class DocumentReader {

    /**
     * Reads a document.
     *
     * @param documentName
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Document getDocument(String documentName) throws IOException, InterruptedException {
        DocumentIndex nameIndex = DocumentIndexCache.getIndex("name");
        if(!nameIndex.containsKey(documentName)) {
            return null;
        }
        FileDataBlockRef ref = nameIndex.getDataBlockRefs(documentName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileDataReader reader = new FileDataReader(ref.getFilename());
        List<FileDataBlock> blocks = reader.getDataBlocks(ref.getInitialBlockId(), ref.getByteOffset(),
                ref.numberOfBlocks(), ref.getByteLength());
        for(FileDataBlock block : blocks) {
            baos.write(block.getData());
        }
        baos.close();
        if(baos.size() == 0) {
            return null;
        }
        byte[] document = FileDataRecordFormat.decode(baos.toByteArray());
        return Document.deserialize(document);
    }
}
