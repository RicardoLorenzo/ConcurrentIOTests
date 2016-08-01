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
import conf.Configuration;
import data.Document;
import data.DocumentReader;
import data.DocumentWriter;
import file.ConcurrentFileTextSearch;
import io.FileDataWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

public class ConcurrentIOTests {

    public static void main(String[] args) {
        /**
         * Concurrent MMAP text file search
         */
        concurrentMmapFileTextSearch();
        try {
            simpleDocumentDatabase();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void concurrentMmapFileTextSearch() {
        ConcurrentFileTextSearch concurrentFileTextSearch = new ConcurrentFileTextSearch();
        try {
            File f = Configuration.getResource("mmapfile.txt");
            String match = "hi";

            System.out.println();
            System.out.print("Searching for \"");
            System.out.print(match);
            System.out.print("\" in [");
            System.out.print(f.getName());
            System.out.println("]:");
            List<String> resultLines = concurrentFileTextSearch.search(f, match, 4);
            resultLines.stream().forEach(line -> {
                System.out.print("M: ");
                System.out.println(line);
            });
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void simpleDocumentDatabase() throws InterruptedException {
        List<Document> documents = new ArrayList<>();
        List<String> documentNames = Arrays.asList(new String[] { "test1", "test2", "test3", "test4",
          "test5", "test6" });

        for(Integer i = 0; i < documentNames.size(); i++) {
            Document doc = new Document(i.longValue(), documentNames.get(i));
            if(i % 2 == 0) {
                /*
                 * ~ 10K document size
                 */
                doc.setContent(getRandomCharacters(10240));
            } else {
                /*
                 * ~ 512 document size
                 */
                doc.setContent(getRandomCharacters(512));
            }
            documents.add(doc);
        }

        deleteDataFiles();
        try {
            DocumentWriter writer = new DocumentWriter();
            writer.write(documents);

            Random r = new Random();
            String randomDocumentName = documentNames.get(r.nextInt(documentNames.size() - 1));
            System.out.println("Retrieving document [" + randomDocumentName + "]");
            DocumentReader reader = new DocumentReader();
            Document doc = reader.getDocument(randomDocumentName);
            printDocument(doc);

            FileDataWriter.shutdownWriteThreads();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printDocument(Document doc) {
        System.out.println();
        System.out.println("Document: ");
        System.out.println("========");
        System.out.println(doc);
        System.out.println();
    }

    private static void deleteDataFiles() {
        File directory = new File(Configuration.getResourcePath());
        for(File f : directory.listFiles()) {
            if(f.isFile() && (f.getName().endsWith(".dat") ||
                    f.getName().endsWith(".idx"))) {
                f.delete();
            }
        }
    }

    private static String getRandomCharacters(Integer length) {
        Character[] characters = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
        'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9' };

        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for(Integer i = 0; i < length; i++) {
            sb.append(characters[r.nextInt(characters.length)]);
        }
        return sb.toString();
    }
}