package sorter;

import com.drew.metadata.Metadata;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.net.URLDecoder.decode;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Test for image sorter code
 */
public class ImageSorterTest {

    private static final String[] TEST_IMAGES = {"IMG_20130316_233621.jpg",
            "2015-05-14 12.46.44.jpg", "2015-05-14 12.46.37.jpg",
            "2015-05-14 12.46.33.jpg", "2015-05-14 12.46.28.jpg"};
    private static final String TEST_IMAGE = "IMG_20130316_233621.jpg";
    private static final String TEST_VIDEO = "VID_20151109_115010.mp4";
    private static final LocalDateTime IMAGE_DATE = LocalDateTime.of(2013, 3, 16, 23, 36, 21);
    private ImageSorter sorter;
    private URL testImage, testVideo;
    private File[] testFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * TODO need to copy the source files into a temporary folder for each test as they get moved now
     * and are not able to be used again for other tests.
     */
    @Rule
    public TemporaryFolder tempSourceImagesFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        testImage = ClassLoader.getSystemResource(TEST_IMAGE);
        assertNotNull("Test image " + TEST_IMAGE + " does not exist!", testImage.getPath());
        testVideo = ClassLoader.getSystemResource(TEST_VIDEO);
        assertNotNull("Test video " + TEST_VIDEO + " does not exist!", testVideo.getPath());
        sorter = new ImageSorter(testImage.getPath());

        // Prepare the files for testing
        testFile = new File[5];
        for (int i = 1; i < 5; i++) {
            URL url = ClassLoader.getSystemResource(TEST_IMAGES[i]);
            assertNotNull("Image for path '" + TEST_IMAGES[i] + "' was missing", url);
            String path = decode(url.getPath(), "UTF-8");
            assertNotNull("Path to image" + TEST_IMAGES[i] + " was null", path);
            testFile[i] = new File(path);
            assertNotNull("Test file from was null", testFile[i]);
            assertTrue("Test file should have been created", testFile[i].exists());
            assertTrue("Test file should be file", testFile[i].isFile());
        }
    }

    /**
     * First test that prints out all the meta data for an image
     *
     * @throws Exception
     */
    @Test
    public void testGetMetaDataForImage() throws Exception {

        Metadata imageData = sorter.getInfoForImage(testImage.getPath());
        assertNotNull("Metadata should not be null", imageData);
    }

    /**
     * Get the meta data for a video - this is not supported from the library
     * So will have to be implemented with something else
     *
     * @throws Exception
     */
    @Test
    @Ignore("The library does not support video files yet")
    public void testGetMetaDataForVideo() throws Exception {

        Metadata videoData = sorter.getInfoForImage(testVideo.getPath());
        assertNotNull("Metadata should not be null", videoData);
    }

    /**
     * Check that the date we get back is the same as the image creation date
     *
     * @throws Exception
     */
    @Test
    public void testGetDateForImage() throws Exception {

        LocalDateTime date = sorter.getImageCreationDate(testImage.getPath());
        assertNotNull("Returned date should not be null", date);

        // Check fields
        assertThat(date.getYear(), is(IMAGE_DATE.getYear()));
        assertThat(date.getMonth(), is(IMAGE_DATE.getMonth()));
        assertThat(date.getDayOfMonth(), is(IMAGE_DATE.getDayOfMonth()));
        assertThat(date.getHour(), is(IMAGE_DATE.getHour()));
        assertThat(date.getMinute(), is(IMAGE_DATE.getMinute()));
        assertThat(date.getSecond(), is(IMAGE_DATE.getSecond()));

        assertEquals("Extracted date for image was incorrect", IMAGE_DATE, date);
    }

    /**
     * Test that we get the right time stamp for a given date
     *
     * @throws Exception
     */
    @Test
    public void testTimeStampFormatForGivenDate() throws Exception {

        String stamp = sorter.getTimeStampFormatForDate(IMAGE_DATE);
        assertNotNull(stamp);
        assertEquals("Stamp is not of the right format", "2013_03_16 - ", stamp);

        final LocalDateTime singleDigitDate = LocalDateTime.of(1999, 1, 1, getRandomNumberBetween(1, 12),
                getRandomNumberBetween(1, 60));

        stamp = sorter.getTimeStampFormatForDate(singleDigitDate);
        assertNotNull(stamp);
        assertEquals("Stamp is not of the right format", "1999_01_01 - ", stamp);
    }

    /**
     * Test the creation of a map that contains the right number of files to timestamps
     *
     * @throws Exception
     */
    @Test
    public void testMapOfFilesForTestDataContainsCorrectData() throws Exception {

//        Stream<Path> filesList = Files.list(Paths.get(testImage.getPath()));
        File a = new File(testImage.getPath());

        String[] fileExtensions = {"jpg"};
        Collection<File> imageFileList = FileUtils.listFiles(a.getParentFile(), fileExtensions, false);

        Map<String, List<File>> sortedResult = sorter.mapDatesToFiles(imageFileList);

        assertNotNull("Returned map should not be null", sortedResult);
        assertFalse("Returned map should not be empty", sortedResult.isEmpty());

        // Check the results in the map based on the contents of the test files
        /**
         * 0 = {HashMap$Node@1268} "2013_03_16 - " -> " size = 1"
         * 1 = {HashMap$Node@1269} "2015_04_27 - " -> " size = 1"
         * 2 = {HashMap$Node@1270} "2015_04_23 - " -> " size = 1"
         * 3 = {HashMap$Node@1237} "2015_05_14 - " -> " size = 4"
         */
        assertThat("After sorting there should be 4 dates in the map", sortedResult.size(), is(4));
        final String KEY_1_2013_03_16 = "2013_03_16 - ";
        final String KEY_2_2015_04_27 = "2015_04_27 - ";
        final String KEY_3_2015_04_23 = "2015_04_23 - ";
        final String KEY_4_2015_05_14 = "2015_05_14 - ";
        assertThat(KEY_1_2013_03_16 + " Should be in the sorted map", sortedResult.containsKey(KEY_1_2013_03_16), is(true));
        assertThat(KEY_2_2015_04_27 + " Should be in the sorted map", sortedResult.containsKey(KEY_2_2015_04_27), is(true));
        assertThat(KEY_3_2015_04_23 + " Should be in the sorted map", sortedResult.containsKey(KEY_3_2015_04_23), is(true));
        assertThat(KEY_4_2015_05_14 + " Should be in the sorted map", sortedResult.containsKey(KEY_4_2015_05_14), is(true));

        // TODO assert the contents of the map for each key - this will be used in the final test
//        assertThat(sortedResult.get(KEY_1_2013_03_16), hasItem())

        // TODO get the sum of the files to count them?
    }

    /**
     * Implement this when the mapping is sorted
     *
     * @throws Exception
     */
    @Test
    public void testCreatingTimeStampFolder() throws Exception {

        // Get a temporary folder
        File rootFolder = temporaryFolder.getRoot();

        // Get a map of sorted Files
        Map<String, List<File>> sortedMap = new HashMap<>();
        sortedMap.put("2016_03_10 - ", Arrays.asList(testFile[0], testFile[1]));
        sortedMap.put("2016_03_09 - ", Arrays.asList(testFile[2], testFile[3]));
        sortedMap.put("2016_03_08 - ", Arrays.asList(testFile[4]));

        Map<String, List<File>> movedFiles = sorter.sortImages(sortedMap, rootFolder);

        assertNotNull("Moved files should not be null", movedFiles);
    }

    /**
     * Generate a random number between a min and max. This is used to test the timestamps
     *
     * @param min int min
     * @param max int max
     * @return int random number between the bounds (TODO check the edges)
     */
    private int getRandomNumberBetween(int min, int max) {
        // nextInt is normally exclusive of the top value, so add 1 to make it inclusive
        int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);

        return randomNum;
    }
}