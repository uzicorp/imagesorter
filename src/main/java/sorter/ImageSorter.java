package sorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.extern.slf4j.Slf4j;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.apache.commons.io.FileUtils;

/**
 * Create an instance of an image sorter that will read a directory, its subdirectores (maybe) and then sort the images
 * based on the day they were taken
 */
@Slf4j
public class ImageSorter {

    public ImageSorter(String path) { // TODO This should be a folder and not a path?
        // If you get a valid folder then begin processing the folder
        // add recursive option?
    }

    /**
     * No args constructor
     */
    public ImageSorter() {
    }

    /**
     * Development method to check what metadata we can extract from an image.
     * Eventually this should be private
     *
     * @param path String path of image
     * @return Metadata object as part of the internal library api
     * @throws Exception if file does not exist
     */
    public Metadata getInfoForImage(String path) throws Exception {

        File jpegFile = getFile(path);

        Metadata metadata;

        try {
            metadata = ImageMetadataReader.readMetadata(jpegFile);
            // or Metadata metadata = ImageMetadataReader.readMetadata(stream);

            // Go through all the metadata items and log them
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    log.info("tag - {}", tag);
                }
            }

        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return metadata;
    }

    /**
     * Given a path of an image, get the date that it was created
     *
     * @param path String path
     * @return LocalDateTime time of creation
     */
    public LocalDateTime getImageCreationDate(String path) throws FileNotFoundException {

        File imageFile = getFile(path);
        LocalDateTime ldt = null;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            // obtain the Exif directory
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            // query the tag's value
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            log.info("Date extracted = {}", date);

            ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            log.info("Converted datetime = {}", ldt);

        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
            // TODO add some more meaningful error handling
        }
        return ldt;
    }

    /**
     * Private utility methods below
     */

    /**
     * Get a file, given a path
     *
     * @param path String path
     * @return File referring to the path
     * @throws FileNotFoundException
     * @throws UnsupportedOperationException
     */
    private File getFile(String path) throws FileNotFoundException {
        File jpegFile = new File(path);

        if (!jpegFile.exists()) {
            throw new FileNotFoundException("File at " + path + " cannot be found");
        } else if (jpegFile.isDirectory()) {
            throw new UnsupportedOperationException("Directories are not supported yet"); // TODO allow the files in a dir to be searched
        }
        return jpegFile;
    }

    /**
     * Given a date, convert it into a timestamp format we can use to create folders from
     *
     * @param date
     * @return
     */
    protected String getTimeStampFormatForDate(LocalDateTime date) {

        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        String stamp = new StringBuilder()
                .append(String.format("%02d", year))
                .append("_")
                .append(String.format("%02d", month))
                .append("_")
                .append(String.format("%02d", day))
                .append(" - ")
                .toString();

        log.info("Stamp for {} = {}", date, stamp);
        return stamp;
    }

    /**
     * For each file, organise the files according to their datestamp in the format of 'YYYY_MM_DD -'
     *
     * @param
     * @return
     */
    protected Map<String, List<File>> mapDatesToFiles(Collection<File> filesCollection) throws FileNotFoundException {

        Map<String, List<File>> sortedFilesMap = new HashMap<>(filesCollection.size());

        for (File file : filesCollection) {
            // Get date for file
            LocalDateTime dateForFile = getImageCreationDate(file.getPath());
            String timeStamp = getTimeStampFormatForDate(dateForFile);
            log.info("Date for file {} = {} ==> {}", file.getName(), dateForFile, timeStamp);

            if (sortedFilesMap.get(timeStamp) == null) {
                // Timestamp has not been seen before so create a new entry with the file
                List<File> fileForDate = new ArrayList<>();
                fileForDate.add(file);
                sortedFilesMap.put(timeStamp, fileForDate);
                log.info("Added {} to new slot {} in map", file.getName(), timeStamp);
            } else {
                // Already in the map so just add the file to it
                List<File> existingStampList = sortedFilesMap.get(timeStamp);
                existingStampList.add(file);
                log.info("{} now has {} files in it", timeStamp, existingStampList.size());
            }
        }

        log.info("There are now {} stamps in the map", sortedFilesMap.size());
        return sortedFilesMap;
    }

    /**
     * Move the files into the mapped folders
     *
     * @param sortedMap  Map of sorted files keyed with the folder timestamp
     * @param rootFolder base folder (not file) of
     * @return
     */
    protected Map<String, List<File>> sortImages(Map<String, List<File>> sortedMap, File rootFolder)
            throws Exception {

        if (!rootFolder.isDirectory()) {
            log.error("Folder {} was not a directory", rootFolder);
            throw new IllegalArgumentException("Provided root folder is not a directory");
        } else if (!rootFolder.exists()) {
            if (!rootFolder.mkdirs()) {
                log.error("Tried to create folders for {} but there was an error", rootFolder);
                throw new IllegalStateException("Couldn't create dir: " + rootFolder);
            } else {
                log.info("Root folder at {} was created", rootFolder.getAbsolutePath());
            }
        }

        log.info("Root folder is '{}'", rootFolder.getAbsolutePath());

        // Perform the sorting - Create the folder at the base of the root folder
        for (String folder : sortedMap.keySet()) {
            log.info("Processing folder '{}' has {} files", folder, sortedMap.get(folder).size());

            File directory = new File(rootFolder, folder);
            if (directory.mkdir()) {
                log.info("Path at {} was created", directory.getAbsolutePath());

                // Move each file into that directory?
                List<File> files = sortedMap.get(folder);
                for (File file : files) {
                    FileUtils.moveFileToDirectory(file, directory, true);
                }
            } else {
                log.error("Path at {} was not created", directory);
                throw new Exception("Path at " + directory + "was not created");
            }
        }
        return null;
    }
}
