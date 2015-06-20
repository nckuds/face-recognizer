package net.spright.hdfs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.IntBuffer;
import static org.bytedeco.javacpp.opencv_contrib.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

/**
 * I couldn't find any tutorial on how to perform face recognition using OpenCV and Java,
 * so I decided to share a viable solution here. The solution is very inefficient in its
 * current form as the training model is built at each run, however it shows what's needed
 * to make it work.
 *
 * The class below takes two arguments: The path to the directory containing the training
 * faces and the path to the image you want to classify. Not that all images has to be of
 * the same size and that the faces already has to be cropped out of their original images
 * (Take a look here http://fivedots.coe.psu.ac.th/~ad/jg/nui07/index.html if you haven't
 * done the face detection yet).
 *
 * For the simplicity of this post, the class also requires that the training images have
 * filename format: <label>-rest_of_filename.png. For example:
 *
 * 1-jon_doe_1.png
 * 1-jon_doe_2.png
 * 2-jane_doe_1.png
 * 2-jane_doe_2.png
 * ...and so on.
 *
 * @author Petter Christian Bjelland
 * @author chihsuan
 */
public class FaceReg {
    public static void main(String[] args) throws IOException {
        
        final FileSystem fs = getFileSystem();
        String trainingDir = ""; //args[0];
        Mat testImage = imread(args[0], CV_LOAD_IMAGE_GRAYSCALE);
        resize(testImage, testImage, new Size(200, 200));
        File[] imageFiles = null;
        try {
          imageFiles = initImageList(fs, trainingDir); 
          matchImg(imageFiles, testImage);
        } catch (IOException ex) {
           System.out.println(ex);
        }
    }
    
    private static void matchImg (File[] imageFiles, Mat testImage) {

        MatVector images = new MatVector(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.getIntBuffer();

        int counter = 0;
        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
            resize(img, img, new Size(200, 200));
           
            int label = Integer.parseInt(image.getName().split("\\-")[0]);
            
            images.put(counter, img);

            labelsBuf.put(counter, label);

            counter++;
        }

        FaceRecognizer faceRecognizer = createFisherFaceRecognizer();
        // FaceRecognizer faceRecognizer = createEigenFaceRecognizer();
        // FaceRecognizer faceRecognizer = createLBPHFaceRecognizer()

        faceRecognizer.train(images, labels);

        int predictedLabel = faceRecognizer.predict(testImage);

        System.out.println("Predicted label: " + predictedLabel);
    }
    
        
    private static File []  initImageList(FileSystem fs, String trainingDir) 
            throws IOException {
        File root = new File("hdfs://course/user/course");   
        FilenameFilter imgFilter;
        imgFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };
        File [] images = root.listFiles(imgFilter);
         
        return images;
    }
    
    private static FileSystem getFileSystem() throws IOException {
       Configuration configuration = new Configuration() {};
       FileSystem fs = FileSystem.get(configuration);
       return fs;
    }
    
}