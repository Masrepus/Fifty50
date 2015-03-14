package com.fifty50.computer.HSVDetector;
// ColorRectDetector.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, July 2013

/* This detector uses JavaCV (OpenCV) to find the largest bounded box
   for a specified HSV colour in a webcam image supplied by the call to
   findRect(). The box's center point and angle of its longest side to the
   horizontal are also calculated.
*/

import java.io.*;
import java.awt.*;
import java.awt.image.*;

import com.googlecode.javacv.*;
import com.googlecode.javacv.cpp.*;
import com.googlecode.javacpp.Loader;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;



public class ColorRectDetector
{
  private static final int NUM_POINTS = 4;  // number of coords in box

  // default HSV initial slider ranges
  private static final int HUE_LOWER = 0;
  private static final int HUE_UPPER = 179;
             // the Hue component ranges from 0 to 179 (not 255)
     /* this is documented at:
            http://opencv.willowgarage.com/documentation/cpp/
                 miscellaneous_image_transformations.html?highlight=hsv#cvtcolor
                 under RGB <--> HSV for 8-bit images
        also see: "HSV Color format in OpenCV" at
            http://www.shervinemami.co.cc/colorConversion.html
            (his HSV Color Wheel application is useful)
     */
  private static final int SAT_LOWER = 0;
  private static final int SAT_UPPER = 255;

  private static final int BRI_LOWER = 0;
  private static final int BRI_UPPER = 255;


  private static final float SMALLEST_BOX =  600.0f;    // was 100.0f;
            // ignore detected boxes smaller than SMALLEST_BOX pixels

  // HSV ranges defining the colour being detected by this object
  private int hueLower, hueUpper, satLower, satUpper, briLower, briUpper;

  // OpenCV elements
  private CvMemStorage storage;
  private IplImage hsvImg;  // HSV version
  private IplImage imgThreshed;  // threshold for HSV settings

  // bounded box details
  private boolean foundBox = false;
  private int[] xPoints, yPoints;
  private Point center;
  private int angle;     // to the horizontal (in degrees)



  public ColorRectDetector(int width, int height)
  {
    hsvImg = IplImage.create(width, height, 8, 3);     // for the HSV image
    imgThreshed = IplImage.create(width, height, 8, 1);   // threshold image

    storage = CvMemStorage.create();

    // storage for the coordinates of the bounded box
    xPoints = new int[NUM_POINTS];
    yPoints = new int[NUM_POINTS];
    center = new Point();
    angle = 0;

    // set default HSV ranges
    hueLower = HUE_LOWER;  hueUpper = HUE_UPPER;
    satLower = SAT_LOWER;  satUpper = SAT_UPPER;
    briLower = BRI_LOWER;  briUpper = BRI_UPPER;
  }  // end of ColorRectDetector()



  public boolean findRect(IplImage im)
 /* Convert the image into an HSV version. Calculate a threshold
    image using the HSV ranges for the colour being detected. Find
    the largest bounded box in the threshold image.
 */
  {
    if (im == null)
      return false;

    // convert to HSV
    cvCvtColor(im, hsvImg, CV_BGR2HSV);

    // threshold image using supplied HSV settings
    cvInRangeS(hsvImg, cvScalar(hueLower, satLower, briLower, 0),
                       cvScalar(hueUpper, satUpper, briUpper, 0), imgThreshed);

    cvMorphologyEx(imgThreshed, imgThreshed, null, null, CV_MOP_OPEN, 1);
        // do erosion followed by dilation on image to remove specks of white & retain size

    CvBox2D maxBox = findBiggestBox(imgThreshed);

    // extract box details
    if (maxBox != null) {
      foundBox = true;
      extractBoxInfo(maxBox);
    }
    else 
      foundBox = false;

    return foundBox;
  }  // end of findRect()



  private CvBox2D findBiggestBox(IplImage imgThreshed)
  // return the bounding box for the largest contour in the threshold image
  {
    CvSeq bigContour = null;

    // generate all the contours in the threshold image as a list
    CvSeq contours = new CvSeq(null);
    cvFindContours(imgThreshed, storage, contours, Loader.sizeof(CvContour.class),
                                                CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

    // find the largest box in the list of contours
    float maxArea = SMALLEST_BOX;
    CvBox2D maxBox = null;
    while (contours != null && !contours.isNull()) {
      if (contours.elem_size() > 0) {
        CvBox2D box = cvMinAreaRect2(contours, storage);
        if (box != null) {
          CvSize2D32f size = box.size();
          float area = size.width() * size.height();
          if (area > maxArea) {
            maxArea = area;
            maxBox = box;
            bigContour = contours;
          }
        }
      }
      contours = contours.h_next();
    }
    return maxBox;
  }  // end of findBiggestBox()



  private void extractBoxInfo(CvBox2D maxBox)
  /* extracted box information includes: its center, the coordinates of its
     corners, and its angle to the vertical. 
  */
  {
    // the box's center point
    CvPoint2D32f boxCenter = maxBox.center();
    center.x = Math.round( boxCenter.x());
    center.y = Math.round( boxCenter.y());

    CvPoint2D32f box_vtx =  new CvPoint2D32f(NUM_POINTS);
          // allocate native array using an integer as argument
    cvBoxPoints(maxBox, box_vtx);

    // the box's corner coordinates
    for (int i = 0; i < NUM_POINTS; i++) {
      box_vtx.position(i);    // use position() method
      xPoints[i] = (int)Math.round( box_vtx.x() );
      yPoints[i] = (int)Math.round( box_vtx.y() );
    }

    angle = calcAngle(xPoints, yPoints); 
  }  // end of extractBoxInfo()



  private int calcAngle(int[] xPts, int[] yPts)
  /* Calculate the box's degree angle to the horizontal axis for its 
     longest side. Find the longest side relative to the box's lowest
     coordinate on screen. 
  */
  {
    // find index of point with 'largest' y-value (lowest on screen)
    int idxLowest = 0;
    int yLowest = -1;
    for (int i = 0; i < NUM_POINTS; i++) {
      if (yPts[i] > yLowest) {  // further down screen has larger y-coord
        yLowest = yPts[i];
        idxLowest = i;
      }
    }

    // get neigbouring point indicies
    int idxRight = (idxLowest+1)%4;
    int idxLeft = (idxLowest+3)%4;      // same as -1 but remains +ve

    // calculate length^2 of neighbouring sides
    float x = xPts[idxLowest];
    float y = yPts[idxLowest];

    float xRight = xPts[idxRight];
    float yRight = yPts[idxRight];
    float rightLen2 = (yRight-y)*(yRight-y) + (xRight-x)*(xRight-x);   

    float xLeft = xPts[idxLeft];
    float yLeft = yPts[idxLeft];
    float leftLen2 = (yLeft-y)*(yLeft-y) + (xLeft-x)*(xLeft-x);

    // store info about the pt along the longest side from the lowest pt
    int longIdx;
    float xLong, yLong;
    if (rightLen2 > leftLen2) {    // right side is longest
      longIdx = idxRight;
      xLong = xRight;
      yLong = yRight;
    }
    else {  //left side is longest
      longIdx = idxLeft;
      xLong = xLeft;
      yLong = yLeft;
    }

    // calculate angle of longest side to the horizontal
    double radAngle = Math.atan2( (double)(y-yLong), (double)(xLong-x) );
    int angle = (int) Math.round( Math.toDegrees(radAngle) );
    // System.out.println(" angle is " + angle);

    return angle;
  }  // end of calcAngle()



  private void extractContourInfo(CvSeq contour)
  // use the contour to get the center and angle
  {
    CvMoments moments = new CvMoments();
    cvMoments(contour, moments, 1);     // CvSeq is a subclass of CvArr

    // center of gravity
    double m00 = cvGetSpatialMoment(moments, 0, 0) ; 
    double m10 = cvGetSpatialMoment(moments, 1, 0) ; 
    double m01 = cvGetSpatialMoment(moments, 0, 1); 

    if (m00 != 0) {   // calculate center
      int xCenter = (int) Math.round(m10/m00); 
      int yCenter = (int) Math.round(m01/m00);
      System.out.println("COG: (" + xCenter + ", " + yCenter + ")" );
    }

    // angle of major axis to the horizontal, with positive y going down screen
	double m11 = cvGetCentralMoment(moments, 1, 1);
	double m20 = cvGetCentralMoment(moments, 2, 0);
	double m02 = cvGetCentralMoment(moments, 0, 2);

    double theta = 0.5 * Math.atan2(2*m11, m20-m02);
    int thetaDeg = (int) Math.round( Math.toDegrees(theta));
    System.out.println("moment angle: " + thetaDeg);
  }  // end of extractContourInfo()



  public Polygon getBoundedBox()
  {  return ((foundBox) ? new Polygon(xPoints, yPoints, NUM_POINTS) : null);  }

  public Point getCenter()
  {  return ((foundBox) ? center : null);  }

  public int getAngle()
  {  return angle;  }

  
  public BufferedImage getHSVImage()
  {  return hsvImg.getBufferedImage();  }

  public BufferedImage getThresholdImage()
  {  return imgThreshed.getBufferedImage();  }


  // -------------- set HSV ranges --------------------

  public void setHueRange(int lower, int upper)
  {  hueLower = lower;  
     hueUpper = upper;
  }

  public void setSatRange(int lower, int upper)
  {  satLower = lower;  
     satUpper = upper;
  }

  public void setBriRange(int lower, int upper)
  {  briLower = lower;  
     briUpper = upper;
  }

}  // end of ColorRectDetector class
