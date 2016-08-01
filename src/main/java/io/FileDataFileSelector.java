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

import conf.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * Selects the appropriate data file when adding records.
 *
 * @author Ricardo Lorenzo <unshakablespirit@gmail.com>
 */
public class FileDataFileSelector {

    /**
     * File is selected automatically if additional documents are written. A different file is picked
     * if the file size reaches the maximum size.
     *
     * @return
     * @throws IOException
     */
    public static String selectFileName() throws IOException {
        Integer nextFileNumber = 1;
        File directory = new File(Configuration.getResourcePath());

        for(File f : directory.listFiles()) {
            if(f.isFile() && f.getName().endsWith(".dat")) {
                String fileName = f.getName();
                fileName = fileName.substring(Configuration.FILENAME_DATA_PREFIX.length());
                fileName = fileName.substring(0, fileName.length() - Configuration.FILENAME_DATA_SUFFIX.length());
                if(Integer.valueOf(fileName) > nextFileNumber) {
                    nextFileNumber = Integer.valueOf(fileName);
                };
            }
        }

        StringBuilder filename = new StringBuilder();
        filename.append(Configuration.FILENAME_DATA_PREFIX);
        filename.append(nextFileNumber);
        filename.append(Configuration.FILENAME_DATA_SUFFIX);

        File f = new File(directory.getAbsolutePath() + File.separator + filename.toString());
        if(f.length() >= Configuration.FILE_DATA_MAX_SIZE) {
            filename = new StringBuilder();
            filename.append(Configuration.FILENAME_DATA_PREFIX);
            filename.append(nextFileNumber);
            filename.append(Configuration.FILENAME_DATA_SUFFIX);
        }

        f = new File(directory.getAbsolutePath() + File.separator + filename.toString());
        if(!f.exists()) {
            f.createNewFile();
        }

        return filename.toString();
    }
}
