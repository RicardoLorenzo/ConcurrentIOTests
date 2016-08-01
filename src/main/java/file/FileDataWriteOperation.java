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
import data.Document;
import io.FileDataFileSelector;
import io.FileDataReader;
import io.FileDataRecordFormat;
import io.FileDataWriter;
import memory.DocumentIndexCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs the file write operations.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataWriteOperation {
    public static final Integer INSERT = 1;
    public static final Integer UPDATE = 2;

    /**
     * Performs the write in relation to the operation type.
     *
     * @param operationType
     * @param document
     * @param ref
     * @throws IOException
     * @throws InterruptedException
     */
    public void write(final Integer operationType, final Document document, FileDataBlockRef ref)
            throws IOException, InterruptedException {
        switch (operationType) {
            case 1: {
                insert(FileDataFileSelector.selectFileName(), document);
                break;
            }
            case 2: {
                update(document, ref);
                break;
            }
        }
    }

    private static void flushDataToDisk(String dataFileName, Document document, FileDataBlockRef ref, List<FileDataBlock> blocks)
            throws IOException, InterruptedException {
        FileDataWriter writer = new FileDataWriter(dataFileName);
        /*
         * Updating the indexes and the data files
         */
        DocumentIndexCache.setIndexKey("id", document.getID().toString(), ref);
        DocumentIndexCache.setIndexKey("name", document.getName(), ref);
        writer.writeDataBlocks(blocks);
        DocumentIndexCache.flushToDisk();
    }

    private void insert(final String dataFileName, final Document document) throws IOException, InterruptedException {
        Integer byteOffset = 0, byteLength, refByteOffset = 0;
        Long nextBlockId = 0L, refBlockId = 0L;
        List<FileDataBlock> newBlocks = new ArrayList<>();

        /*
         * Gets the on-disk format for the record data.
         */
        byte[] documentData = Document.serialize(document);
        documentData = FileDataRecordFormat.encode(document.getName(), documentData);

        File dataFile = Configuration.getResource(dataFileName);
        if(dataFile != null && dataFile.length() > 0) {
            /**
             * Locating the last data block and position in order to start writing
             */
            FileDataReader reader = new FileDataReader(dataFileName);
            FileDataBlock lastBlock = reader.getLastDataBlock();
            if(lastBlock == null) {
                throw new IOException("invalid last block");
            }
            refByteOffset = lastBlock.getDataLength();
            refBlockId = lastBlock.getID();
            byteLength = Configuration.BLOCK_SIZE - lastBlock.getDataLength();
            if(documentData.length < byteLength) {
                byteLength = documentData.length;
            }
            if(byteLength > 0) {
                lastBlock.writeAt(documentData, byteOffset, lastBlock.getDataLength(), byteLength);
                byteOffset += byteLength;
            }
            nextBlockId = lastBlock.getID() + 1;
            newBlocks.add(lastBlock);
        }

        /**
         * If the record is bigger than the available space in the block, it keeps writing the
         * data on new blocks.
         */
        for(; byteOffset < documentData.length; nextBlockId++) {
            byteLength = documentData.length - byteOffset;
            if(byteLength > Configuration.BLOCK_SIZE) {
                byteLength = Configuration.BLOCK_SIZE;
            }
            FileDataBlock additionalBlock = new FileDataBlock(nextBlockId, documentData, byteOffset,
                    byteLength);
            byteOffset += byteLength;
            newBlocks.add(additionalBlock);
        }

        FileDataBlockRef newRef = new FileDataBlockRef(dataFileName, refBlockId,
                refByteOffset, documentData.length);

        flushDataToDisk(dataFileName, document, newRef, newBlocks);
    }

    private void update(Document document, FileDataBlockRef ref) throws IOException, InterruptedException {
        /*
         * Gets the on-disk format for the record data.
         */
        byte[] documentData = Document.serialize(document);
        documentData = FileDataRecordFormat.encode(document.getName(), documentData);

        if(documentData.length > ref.getByteLength()) {
            dataMoveUpdate(document, ref, documentData);
        } else {
            inPlaceUpdate(document, ref, documentData);
        }
    }

    private static void inPlaceUpdate(Document document, FileDataBlockRef ref, byte[] documentData) throws IOException,
            InterruptedException {
        /*
         * In this case, the document fits in the current allocated blocks. It could also create some fragmentation
         * if the document shrinks.
         */
        FileDataReader reader = new FileDataReader(ref.getFilename());
        List<FileDataBlock> blocks = reader.getDataBlocks(ref.getInitialBlockId(), 0, ref.numberOfBlocks(),
                Configuration.BLOCK_SIZE);
        List<FileDataBlock> newBlocks = new ArrayList<>();

        for(Integer blockNo = 0; blockNo < blocks.size(); blockNo++) {
            FileDataBlock block = blocks.get(blockNo);
            Integer byteLength, srcByteOffset = 0, dstByteOffset;

            if(blockNo == 0) {
                dstByteOffset = ref.getByteOffset();
                byteLength = Configuration.BLOCK_SIZE - ref.getByteOffset();
                if(documentData.length < byteLength) {
                    byteLength = documentData.length;
                }
            } else {
                dstByteOffset = 0;
                byteLength = documentData.length - srcByteOffset;
                if(byteLength > Configuration.BLOCK_SIZE) {
                    byteLength = Configuration.BLOCK_SIZE;
                }
            }

            block.writeAt(documentData, srcByteOffset, dstByteOffset, byteLength);
            srcByteOffset += byteLength;

            newBlocks.add(block);
        }
        FileDataBlockRef newRef = new FileDataBlockRef(ref.getFilename(), ref.getInitialBlockId(),
                ref.getByteOffset(), documentData.length);

        flushDataToDisk(ref.getFilename(), document, newRef, newBlocks);
    }

    private static void dataMoveUpdate(Document document, FileDataBlockRef ref, byte[] documentData) throws IOException,
            InterruptedException {
        /*
         * Requires a document relocation. The new document will be written at the end of the file
         * in a different block.
         *
         * This will likely cause fragmentation in the files. A defragmentation utility will be required to
         * handle the space efficiently.
         */
        List<FileDataBlock> newBlocks = new ArrayList<>();
        FileDataReader reader = new FileDataReader(ref.getFilename());
        FileDataBlock lastBlock = reader.getLastDataBlock();

        Integer byteLength = Configuration.BLOCK_SIZE - lastBlock.getDataLength();
        Integer byteOffset = 0, lastByteOffset = lastBlock.getDataLength();
        Long nextId = lastBlock.getID() + 1;
        if(documentData.length < byteLength) {
            byteLength = documentData.length;
        }
        if(byteLength > 0) {
            lastBlock.writeAt(documentData, byteOffset, lastBlock.getDataLength(), byteLength);
            byteOffset = byteLength;
        }
        newBlocks.add(lastBlock);

        for(; byteOffset < documentData.length; nextId++) {
            byteLength = documentData.length - byteOffset;
            if(byteLength > Configuration.BLOCK_SIZE) {
                byteLength = Configuration.BLOCK_SIZE;
            }
            FileDataBlock additionalBlock = new FileDataBlock(nextId.longValue(), documentData, byteOffset,
                    byteLength);
            byteOffset += byteLength;
            newBlocks.add(additionalBlock);
        }

        FileDataBlockRef newRef = new FileDataBlockRef(ref.getFilename(), lastBlock.getID(),
                lastByteOffset, documentData.length);

        flushDataToDisk(ref.getFilename(), document, newRef, newBlocks);
    }
}
