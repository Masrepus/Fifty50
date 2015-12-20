package com.fifty50.computer;
// HandDetector.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, July 2013

/* Analyze an image containing an HSV coloured gloved hand.
   Find the largest contour, its convex hull, and convexity
   defects. Extract finger tips from the defects and, by assuming
   that it is a left hand, label the fingers.
*/

import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.Pointer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;


public class HandDetector {
    private static final int IMG_SCALE = 2;  // scaling applied to webcam image

    private static final float SMALLEST_AREA = 50.0f;    // was 600.0f;
    // ignore smaller contour areas

    private static final int MAX_POINTS = 20;   // max number of points stored in an array

    // used for simiplifying the defects list
    private static final int MIN_FINGER_DEPTH = 20;
    private static final int MAX_FINGER_ANGLE = 60;   // degrees

    // angle ranges of thumb and index finger of the left hand relative to its COG
    private static final int MIN_THUMB = 120;
    private static final int MAX_THUMB = 200;

    private static final int MIN_INDEX = 60;
    private static final int MAX_INDEX = 120;


    // HSV ranges defining the glove colour
    private int hueLower, hueUpper, satLower, satUpper, briLower, briUpper;

    // JavaCV elements
    private IplImage scaleImg;     // for resizing the webcam image
    private IplImage hsvImg;       // HSV version of webcam image
    private IplImage imgThreshed;  // threshold for HSV settings
    private CvMemStorage contourStorage, approxStorage, hullStorage, defectsStorage;

    private Font msgFont;

    // hand details
    private Point cogPt;           // center of gravity (COG) of contour
    private int contourAxisAngle;
    // contour's main axis angle relative to the horizontal (in degrees)

    // defects data for the hand contour
    private Point[] tipPts, foldPts;
    private float[] depths;
    private ArrayList<Point> fingerTips;

    // finger identifications
    private ArrayList<FingerName> namedFingers;

    private int x, y, width, height, panelWidth, panelHeight;


    public HandDetector(String hsvPath, int width, int height, int x, int y, int panelWidth, int panelHeight) {
        this.width = width;
        this.height = height;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;

        scaleImg = IplImage.create(width / IMG_SCALE, height / IMG_SCALE, 8, 3);
        hsvImg = IplImage.create(width / IMG_SCALE, height / IMG_SCALE, 8, 3);     // for the HSV image
        imgThreshed = IplImage.create(width / IMG_SCALE, height / IMG_SCALE, 8, 1);   // threshold image

        // storage for contour, hull, and defect calculations by OpenCV
        contourStorage = CvMemStorage.create();
        approxStorage = CvMemStorage.create();
        hullStorage = CvMemStorage.create();
        defectsStorage = CvMemStorage.create();

        msgFont = new Font("SansSerif", Font.BOLD, 18);

        cogPt = new Point();
        fingerTips = new ArrayList<Point>();
        namedFingers = new ArrayList<FingerName>();

        tipPts = new Point[MAX_POINTS];   // coords of the finger tips
        foldPts = new Point[MAX_POINTS];  // coords of the skin folds between fingers
        depths = new float[MAX_POINTS];   // distances from tips to folds

        setHSVRanges(hsvPath);

        this.x = x;
        this.y = y;
    }  // end of HandDetector()


    private void setHSVRanges(String fnm)
  /* read in three lines to set the lower/upper HSV ranges for the user's glove. 
     These were previously stored using the HSV Selector application 
     (see NUI chapter 5 on blobs drumming). */ {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fnm))));
            //BufferedReader in = new BufferedReader(new FileReader(fnm));
            String line = in.readLine();   // get hues
            String[] toks = line.split("\\s+");
            hueLower = Integer.parseInt(toks[1]);
            hueUpper = Integer.parseInt(toks[2]);

            line = in.readLine();   // get saturations
            toks = line.split("\\s+");
            satLower = Integer.parseInt(toks[1]);
            satUpper = Integer.parseInt(toks[2]);

            line = in.readLine();   // get brightnesses
            toks = line.split("\\s+");
            briLower = Integer.parseInt(toks[1]);
            briUpper = Integer.parseInt(toks[2]);

            in.close();
            System.out.println("Read HSV ranges from " + fnm);
        } catch (Exception e) {
            System.out.println("Could not read HSV ranges from " + fnm);
            System.exit(1);
        }
    }  // end of setHSVRanges()


    public void update(IplImage im)
 /* Convert the image to HSV format. Calculate a threshold
    image using the HSV ranges for the colour being detected. Find
    the largest contour in the threshold image. Find the finger tips
    using a convex hull and defects detection, and then label the fingers
    (assuming that the thumb is on the left of the hand).
 */ {

        // scale and convert image format to HSV
        cvResize(im, scaleImg);
        cvCvtColor(scaleImg, hsvImg, CV_BGR2HSV);

        // threshold the image using the loaded HSV settings for the user's glove
        cvInRangeS(hsvImg, cvScalar(hueLower, satLower, briLower, 0),
                cvScalar(hueUpper, satUpper, briUpper, 0), imgThreshed);

        cvMorphologyEx(imgThreshed, imgThreshed, null, null, CV_MOP_OPEN, 1);
        // do erosion followed by dilation on the image to remove specks of white & retain size

        CvSeq bigContour = findBiggestContour(imgThreshed);
        if (bigContour == null)
            return;

        extractContourInfo(bigContour, IMG_SCALE);
        // find the COG and angle to horizontal of the contour

        findFingerTips(bigContour, IMG_SCALE);
        // detect the finger tips positions in the contour

        nameFingers(cogPt, contourAxisAngle, fingerTips);
    }  // end of update()


    private BufferedImage scaleImage(BufferedImage im, int scale)
    // scaling makes the image faster to process
    {
        int nWidth = im.getWidth() / scale;
        int nHeight = im.getHeight() / scale;

        BufferedImage smallIm = new BufferedImage(nWidth, nHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = smallIm.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(im, 0, 0, nWidth, nHeight,
                0, 0, im.getWidth(), im.getHeight(), null);
        g2.dispose();
        return smallIm;
    }  // end of scaleImage()


    private CvSeq findBiggestContour(IplImage imgThreshed)
    // return the largest contour in the threshold image
    {
        CvSeq bigContour = null;

        // generate all the contours in the threshold image as a list
        CvSeq contours = new CvSeq(null);
        cvFindContours(imgThreshed, contourStorage, contours, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

        // find the largest contour in the list based on bounded box size
        float maxArea = SMALLEST_AREA;
        CvBox2D maxBox = null;
        while (contours != null && !contours.isNull()) {
            if (contours.elem_size() > 0) {
                CvBox2D box = cvMinAreaRect2(contours, contourStorage);
                if (box != null) {
                    CvSize2D32f size = box.size();
                    float area = size.width() * size.height();
                    if (area > maxArea) {
                        maxArea = area;
                        bigContour = contours;
                    }
                }
            }
            contours = contours.h_next();
        }
        return bigContour;
    }  // end of findBiggestContour()


    // ----------------- analyze contour ----------------------------


    private void extractContourInfo(CvSeq bigContour, int scale)
  /* calculate COG and angle of the contour's main axis relative to the horizontal.
     Store them in the globals cogPt and contourAxisAngle
  */ {
        CvMoments moments = new CvMoments();
        cvMoments(bigContour, moments, 1);     // CvSeq is a subclass of CvArr

        // center of gravity
        double m00 = cvGetSpatialMoment(moments, 0, 0);
        double m10 = cvGetSpatialMoment(moments, 1, 0);
        double m01 = cvGetSpatialMoment(moments, 0, 1);

        if (m00 != 0) {   // calculate center
            int xCenter = (int) Math.round(m10 / m00) * scale;
            int yCenter = (int) Math.round(m01 / m00) * scale;
            cogPt.setLocation(xCenter, yCenter);
        }

        double m11 = cvGetCentralMoment(moments, 1, 1);
        double m20 = cvGetCentralMoment(moments, 2, 0);
        double m02 = cvGetCentralMoment(moments, 0, 2);
        contourAxisAngle = calculateTilt(m11, m20, m02);
          /* this angle assumes that the positive y-axis
             is down the screen */


        // deal with hand contour pointing downwards
    /* uses fingertips information generated on the last update of
       the hand, so will be out-of-date */
        if (fingerTips.size() > 0) {
            int yTotal = 0;
            for (Point pt : fingerTips)
                yTotal += pt.y;
            int avgYFinger = yTotal / fingerTips.size();
            if (avgYFinger > cogPt.y)   // fingers below COG
                contourAxisAngle += 180;
        }
        contourAxisAngle = 180 - contourAxisAngle;
         /* this makes the angle relative to a positive y-axis that
            runs up the screen */

        //System.out.println("Contour angle: " + contourAxisAngle);
    }  // end of extractContourInfo()


    private int calculateTilt(double m11, double m20, double m02)
  /* Return integer degree angle of contour's major axis relative to the horizontal, 
     assuming that the positive y-axis goes down the screen. 

     This code is based on maths explained in "Simple Image Analysis By Moments", by
     Johannes Kilian, March 15, 2001 (see Table 1 on p.7). 
     The paper is available at:
          http://public.cranfield.ac.uk/c5354/teaching/dip/opencv/SimpleImageAnalysisbyMoments.pdf
  */ {
        double diff = m20 - m02;
        if (diff == 0) {
            if (m11 == 0)
                return 0;
            else if (m11 > 0)
                return 45;
            else   // m11 < 0
                return -45;
        }

        double theta = 0.5 * Math.atan2(2 * m11, diff);
        int tilt = (int) Math.round(Math.toDegrees(theta));

        if ((diff > 0) && (m11 == 0))
            return 0;
        else if ((diff < 0) && (m11 == 0))
            return -90;
        else if ((diff > 0) && (m11 > 0))  // 0 to 45 degrees
            return tilt;
        else if ((diff > 0) && (m11 < 0))  // -45 to 0
            return (180 + tilt);   // change to counter-clockwise angle measure
        else if ((diff < 0) && (m11 > 0))   // 45 to 90
            return tilt;
        else if ((diff < 0) && (m11 < 0))   // -90 to -45
            return (180 + tilt);  // change to counter-clockwise angle measure

        System.out.println("Error in moments for tilt angle");
        return 0;
    }  // end of calculateTilt()


    // ---------------- analyze fingers -------------------------


    private void findFingerTips(CvSeq bigContour, int scale)
  /* Starting with the contour, calculate its convex hull, and its
     convexity defects. Ignore defects that are unlikely to be fingers.
  */ {
        CvSeq approxContour = cvApproxPoly(bigContour, Loader.sizeof(CvContour.class),
                approxStorage, CV_POLY_APPROX_DP, 3, 1);
        // reduce number of points in the contour

        CvSeq hullSeq = cvConvexHull2(approxContour, hullStorage, CV_COUNTER_CLOCKWISE, 0);
        // find the convex hull around the contour

        CvSeq defects = cvConvexityDefects(approxContour, hullSeq, defectsStorage);
        // find the defect differences between the contour and hull
        int defectsTotal = defects.total();
        if (defectsTotal > MAX_POINTS) {
            System.out.println("Only processing " + MAX_POINTS + " defect points");
            defectsTotal = MAX_POINTS;
        }

        // copy defect information from defects sequence into arrays
        for (int i = 0; i < defectsTotal; i++) {
            Pointer pntr = cvGetSeqElem(defects, i);
            CvConvexityDefect cdf = new CvConvexityDefect(pntr);

            CvPoint startPt = cdf.start();
            tipPts[i] = new Point((int) Math.round(startPt.x() * scale),
                    (int) Math.round(startPt.y() * scale));
            // an array containing the coordinates of the finger tips

            CvPoint endPt = cdf.end();
            CvPoint depthPt = cdf.depth_point();
            foldPts[i] = new Point((int) Math.round(depthPt.x() * scale),
                    (int) Math.round(depthPt.y() * scale));
            // an array containing the coordinates of the skin fold between fingers
            depths[i] = cdf.depth() * scale;
            // an array containing the distances from tips to folds
        }

        reduceTips(defectsTotal, tipPts, foldPts, depths);
    }  // end of findFingerTips()


    private void reduceTips(int numPoints, Point[] tipPts, Point[] foldPts, float[] depths)
  /* Narrow in on 'real' finger tips by ignoring shallow defect depths, and tips
     which have too great an angle between their neighbouring fold points.

     Store the resulting finger tip coordinates in the global fingerTips list.
  */ {
        fingerTips.clear();

        for (int i = 0; i < numPoints; i++) {
            if (depths[i] < MIN_FINGER_DEPTH)    // defect too shallow
                continue;

            // look at fold points on either side of a tip
            int pdx = (i == 0) ? (numPoints - 1) : (i - 1);   // predecessor of i
            int sdx = (i == numPoints - 1) ? 0 : (i + 1);     // successor of i
            int angle = angleBetween(tipPts[i], foldPts[pdx], foldPts[sdx]);
            if (angle >= MAX_FINGER_ANGLE)      // angle between finger and folds too wide
                continue;

            // this point probably is a finger tip, so add to list
            fingerTips.add(tipPts[i]);
        }
        // System.out.println("No. of finger tips: " + fingerTips.size());
    }  // end of reduceTips()


    private int angleBetween(Point tip, Point next, Point prev)
    // calulate the angle between the tip and its neigbouring folds (in integer degrees)
    {
        return Math.abs((int) Math.round(
                Math.toDegrees(
                        Math.atan2(next.x - tip.x, next.y - tip.y) -
                                Math.atan2(prev.x - tip.x, prev.y - tip.y))));
    }

    // ------------------------------- identify the fingers -----------------------


    private void nameFingers(Point cogPt, int contourAxisAngle, ArrayList<Point> fingerTips)
  /* Use the finger tip coordinates, and the comtour's COG and axis angle to horizontal
     to label the fingers.

     Try to label the thumb and index based on their likely angle ranges
     relative to the COG. This assumes that the thumb and index finger are on the
     left side of the hand.

     Then label the other fingers based on the order of the names in the FingerName class
  */ { // reset all named fingers to unknown
        namedFingers.clear();
        for (int i = 0; i < fingerTips.size(); i++)
            namedFingers.add(FingerName.UNBEKANNT);

        labelThumbIndex(fingerTips, namedFingers);

        // printFingers("named fingers", namedFingers);
        labelUnknowns(namedFingers);
        // printFingers("revised named fingers", namedFingers);
    }  // end of nameFingers()


    private void labelThumbIndex(ArrayList<Point> fingerTips,
                                 ArrayList<FingerName> nms)
    // attempt to label the thumb and index fingers of the hand
    {
        boolean foundThumb = false;
        boolean foundIndex = false;

      /* the thumb and index fingers will most likely be stored at the end
         of the list, since the contour hull was built in a counter-clockwise 
         order by the call to cvConvexHull2() in findFingerTips(), and I am assuming
         the thumb is on the left of the hand.
         So iterate backwards through the list.
      */
        int i = fingerTips.size() - 1;
        while ((i >= 0)) {
            int angle = angleToCOG(fingerTips.get(i), cogPt, contourAxisAngle);

            // check for thumb
            if ((angle <= MAX_THUMB) && (angle > MIN_THUMB) && !foundThumb) {
                nms.set(i, FingerName.DAUMEN);
                foundThumb = true;
            }

            // check for index
            if ((angle <= MAX_INDEX) && (angle > MIN_INDEX) && !foundIndex) {
                nms.set(i, FingerName.ZEIGE);
                foundIndex = true;
            }
            i--;
        }
    }  // end of labelThumbIndex()


    private int angleToCOG(Point tipPt, Point cogPt, int contourAxisAngle)
  /* calculate angle of tip relative to the COG, remembering to add the
     hand contour angle so that the hand is orientated straight up */ {
        int yOffset = cogPt.y - tipPt.y;    // make y positive up screen
        int xOffset = tipPt.x - cogPt.x;
        // Point offsetPt = new Point(xOffset, yOffset);

        double theta = Math.atan2(yOffset, xOffset);
        int angleTip = (int) Math.round(Math.toDegrees(theta));
        int offsetAngleTip = angleTip + (90 - contourAxisAngle);
        // this addition ensures that the hand is orientated straight up
        return offsetAngleTip;
    }  // end of angleToCOG()


    private void printFingers(String title, ArrayList<FingerName> nms) {
        System.out.println(title);
        for (FingerName name : nms)
            System.out.println("  " + name);
    }


    private void labelUnknowns(ArrayList<FingerName> nms)
    // attempt to label all the unknown fingers in the list
    {
        // find first named finger
        int i = 0;
        while ((i < nms.size()) && (nms.get(i) == FingerName.UNBEKANNT))
            i++;
        if (i == nms.size())   // no named fingers found, so give up
            return;

        FingerName name = nms.get(i);
        labelPrev(nms, i, name);    // fill-in backwards
        labelFwd(nms, i, name);    // fill-in forwards
    }  // end of labelUnknowns()


    private void labelPrev(ArrayList<FingerName> nms, int i, FingerName name)
    // move backwards through fingers list labelling unknown fingers
    {
        i--;
        while ((i >= 0) && (name != FingerName.UNBEKANNT)) {
            if (nms.get(i) == FingerName.UNBEKANNT) {   // unknown finger
                name = name.getPrev();
                if (!usedName(nms, name))
                    nms.set(i, name);
            } else   // finger is named already
                name = nms.get(i);
            i--;
        }
    }  // end of labelPrev()


    private void labelFwd(ArrayList<FingerName> nms, int i, FingerName name)
    // move forward through fingers list labelling unknown fingers
    {
        i++;
        while ((i < nms.size()) && (name != FingerName.UNBEKANNT)) {
            if (nms.get(i) == FingerName.UNBEKANNT) {  // unknown finger
                name = name.getNext();
                if (!usedName(nms, name))
                    nms.set(i, name);
            } else    // finger is named already
                name = nms.get(i);
            i++;
        }
    }  // end of labelFwd()


    private boolean usedName(ArrayList<FingerName> nms, FingerName name)
    // does the fingers list contain name already?
    {
        for (FingerName fn : nms)
            if (fn == name)
                return true;
        return false;
    }  // end of usedName()


    // --------------------------- drawing ----------------------------------

    public void draw(Graphics2D g2d, boolean drawCOGOnly)
    // draw information about the finger tips and the hand COG
    {
        if (fingerTips.size() == 0)
            return;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);  // line smoothing
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(4));  // thick yellow pen

        // label the finger tips in red or green, and draw COG lines to named tips
        g2d.setFont(msgFont);

        int x_offset = x, y_offset = 10;

        int cogPtX_flipped = cogPt.x, cogPtY_flipped;

        /*if (cogPt.x < (width / 2)) cogPtX_flipped = cogPt.x + (((width / 2) - cogPt.x) * 2);
        else cogPtX_flipped = cogPt.x - ((cogPt.x - (width / 2)) * 2);*/
        if (cogPt.y < (width / 2)) cogPtY_flipped = cogPt.y + (((height / 2) - cogPt.y) * 2);
        else cogPtY_flipped = cogPt.y - ((cogPt.y - (height / 2)) * 2);

        //skip this if requested
        if (!drawCOGOnly) {
            for (int i = 0; i < fingerTips.size(); i++) {
                Point pt = fingerTips.get(i);
                int ptX_flipped = pt.x, ptY_flipped;
                //handle the flipped image
                /*if (pt.x < (width / 2)) ptX_flipped = pt.x + (((width / 2) - pt.x) * 2);
                else ptX_flipped = pt.x - ((pt.x - (width / 2)) * 2);*/
                if (pt.y < (height / 2)) ptY_flipped = pt.y + (((height / 2) - pt.y) * 2);
                else ptY_flipped = pt.y - ((pt.y - (height / 2)) * 2);

                if (namedFingers.get(i) == FingerName.UNBEKANNT) {
                    g2d.setPaint(Color.RED);   // unnamed finger tip is red
                    g2d.drawOval(ptX_flipped - 8 + x_offset, ptY_flipped - 8 + y_offset, 16, 16);
                    g2d.drawString("" + i, ptX_flipped + x_offset, ptY_flipped - 10 + y_offset);   // label it with a digit
                } else {   // draw yellow line to the named finger tip from COG
                    g2d.setColor(Color.YELLOW);
                    g2d.drawLine(cogPtX_flipped + x_offset, cogPtY_flipped + y_offset, ptX_flipped + x_offset, ptY_flipped + y_offset);

                    g2d.setColor(Color.GREEN);   // named finger tip is green
                    g2d.drawOval(ptX_flipped - 8 + x_offset, ptY_flipped - 8 + y_offset, 16, 16);

                    //don't draw finger names
                    //g2d.drawString(namedFingers.get(i).toString().toLowerCase(), ptX_flipped + x, pt.y - 10 + y);
                }
            }
        }

        // draw COG
        g2d.setColor(Color.GREEN);
        g2d.fillOval(cogPtX_flipped - 8 + x_offset, cogPtY_flipped - 8 + y_offset, 16, 16);
    }  // end of draw()


    public ArrayList<FingerName> getNamedFingers() {
        return namedFingers;
    }

    public ArrayList<Point> getFingerTips() {
        return fingerTips;
    }

    public Point getCogPoint() {
        return cogPt;
    }

    public int getContourAxisAngle() {
        return contourAxisAngle - 180; //image flipped!
    }

    public Point getCogFlipped() {
        //calculate the cog x coordinate to be used with a flipped image
        double x = cogPt.getX(), y;
        /*if (cogPt.x < (width / 2)) x = cogPt.x + (((width / 2) - cogPt.x) * 2);
        else x = cogPt.x - ((cogPt.x - (width / 2)) * 2);*/
        if (cogPt.y < (height / 2)) y = cogPt.y + (((height / 2) - cogPt.y) * 2);
        else y = cogPt.y - ((cogPt.y - (height / 2)) * 2);

        //the snapIm might have a different size than the full panel so we have to convert the coordinates
        x = (x / width) * panelWidth;
        y = (y / height) * panelHeight;
        return new Point((int) x, (int) y);
    }

}  // end of HandDetector class
