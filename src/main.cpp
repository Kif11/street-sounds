#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

#include <zbar.h>
#include "../include/base_64.h"
#include "../include/qr_chunk.hpp"

#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>


/*
  SortByAngle Comparator Struct
  Sorts points of the QR squares by their angle to the origin, or center of square
*/
struct SortByAngle {
    cv::Point center;
    SortByAngle(cv::Point center) { this->center = center; }
    bool operator () (cv::Point i, cv::Point j) {
      double iAngle = atan2(i.y-center.y, i.x-center.x) - atan2(0, 1);
      double jAngle = atan2(j.y-center.y, j.x-center.x) - atan2(0, 1);
      iAngle = (iAngle > 0 ? iAngle : (2*M_PI + iAngle)) * 360 / (2*M_PI);
      jAngle = (jAngle > 0 ? jAngle : (2*M_PI + jAngle)) * 360 / (2*M_PI);
      return (iAngle < jAngle);
    }
};

/*
  order_points
  Sorts points of the QR squares by their angle to the origin, or center of squares
  returns a vector of points in br, bl, tl, tr order.
*/
std::vector<cv::Point2f> orderPoints(cv::Mat& tmp) {

	// the top-left point will have the smallest sum, whereas
	// the bottom-right point will have the largest sum
  std::vector<cv::Point2f> pts;
  double xCenter = 0;
  double yCenter = 0;
  for (int i = 0; i < 4; i++) {
    pts.push_back(cv::Point(tmp.at<int>(i,0), tmp.at<int>(i,1)));
    xCenter += pts[i].x;
    yCenter += pts[i].y;
  }
  xCenter /= 4;
  yCenter /= 4;
  std::sort(pts.begin(), pts.end(), SortByAngle(cv::Point(xCenter, yCenter)));
  return pts;
}

/*
  MAIN
*/
int main(int argc, char* argv[]) {
  if (argc < 2) {
    std::cout << "Provide the QR image path" << std::endl;
    return -1;
  }
  //read in original image
  cv::Mat imgOrig = cv::imread(argv[1]);
  std::cout<<imgOrig.rows<<" "<<imgOrig.cols<<std::endl;
  int totalImgArea = imgOrig.rows*imgOrig.cols;
  int erodeMatCoeff = imgOrig.rows/121;
  //initialize helper cv::Mat 's
  cv::Mat gray, thresh, threshErode, otsuTemp;
  //convert to grayscale
  cv::cvtColor(imgOrig, gray, CV_RGBA2GRAY);

  //find the best thresholding value using otsu's algorithm
  //we want to only consider the area that contains the qr code paper
  cv::Rect myROI(0.2*imgOrig.cols, 0.2*imgOrig.rows, 0.5*imgOrig.cols, 0.5*imgOrig.rows);
  int otsu_val = cv::threshold(gray(myROI), otsuTemp, 0, 255, cv::THRESH_BINARY|cv::THRESH_OTSU);
  cv::threshold(gray, thresh, otsu_val, 255, cv::THRESH_BINARY);
  // cv::imshow("thresh", thresh);
  // cv::waitKey(10000);
  //25 is good right now

  //erode
  cv::Mat M = cv::Mat::ones(erodeMatCoeff, erodeMatCoeff, CV_8U);
  cv::Point anchor = cv::Point(-1, -1);
  cv::erode(thresh, threshErode, M, anchor, 1, cv::BORDER_CONSTANT, cv::morphologyDefaultBorderValue());
  //find contours
  std::vector<std::vector<cv::Point> > contours;
  cv::findContours(threshErode, contours, cv::RETR_LIST, cv::CHAIN_APPROX_SIMPLE);
  std::vector<cv::Vec4i> hierarchy;

  //iterate through and store valid contours in qrBlocks vector
  std::vector<std::vector<cv::Point2f> > qrBlocks;
  for (int i = 0; i < contours.size(); ++i) {
    cv::Mat poly;
    //approximate the polygon
    cv::approxPolyDP(contours[i], poly, 0.03*cv::arcLength(contours[i],true), true);
    float area = cv::contourArea(contours[i]);

    //filter out polygons with outlier areas , and those that are not quadrilaterals
    if (poly.rows == 4 && area < 0.53*totalImgArea && area > 0.002*totalImgArea) {
      std::vector<cv::Point2f> pts = orderPoints(poly);
      qrBlocks.push_back(pts);
      cv::Scalar color = cv::Scalar( 255,0,0 );
      //drawContours( imgOrig, contours, i, color, 2, 8, hierarchy, 0, cv::Point() );

    }
  }
  // cv::imshow("thresh", imgOrig);
  // cv::waitKey(0);

  //unwarp the images
  std::vector <QrChunk::QrChunk> chunkVec;
  for (int i = 0; i < qrBlocks.size(); i ++){
    std::vector<cv::Point2f>& pts = qrBlocks[i];
    //calculate the width of the new square image
    double widthA = norm(pts[0] - pts[1]);
    double widthB = norm(pts[2] - pts[3]);
    double maxWidth = fmax(widthA, widthB);
    //calculate the height of the new square image
    double heightA = norm(pts[0] - pts[3]);
    double heightB = norm(pts[1] - pts[2]);
    double maxHeight = fmax(heightA, heightB);
    //save the new square coordinates in ptsDst
    std::vector<cv::Point2f> ptsDst;
    ptsDst.push_back(cv::Point(maxWidth - 1, maxHeight - 1));
    ptsDst.push_back(cv::Point(0, maxHeight - 1));
    ptsDst.push_back(cv::Point(0, 0));
    ptsDst.push_back(cv::Point(maxWidth - 1, 0));

    cv::Mat P, imgWarped, imgGray;
    //get perspective transform matrix
    P = cv::getPerspectiveTransform(pts, ptsDst);
    //warp the image given P
    warpPerspective(imgOrig, imgWarped, P, cv::Size(maxWidth, maxHeight) );
    //convert img to gray before feeding into zbar::imageScanner
    cv::cvtColor(imgWarped, imgGray, CV_RGBA2GRAY);
    cv::copyMakeBorder(imgGray, imgGray, 10, 10, 10, 10, cv::BORDER_CONSTANT, 255);

    QrChunk::QrChunk chunk(imgGray);
    chunkVec.push_back(chunk);
    //cv::waitKey(1000);
  }

  //std::vector<bool> scanned(qrBlocks.size(), false); //vector to keep track of successfully scanned QRs
  //std::vector<std::string> message(qrBlocks.size()); //vector to keep track of message in parts
  //initialize the qr barcode scanner
  zbar::ImageScanner scanner;
  scanner.set_config(zbar::ZBAR_NONE, zbar::ZBAR_CFG_ENABLE, 1);

  //iterate over different image scaling coefficients
  for (double dxdy = 0.3; dxdy < 1.5; dxdy+=0.1){
    //break if we have scanned all QRs
    //if (std::find(begin(scanned), end(scanned), false) == end(scanned)) break;
    //resize and decode the qrblocks
    for (int i = 0; i < chunkVec.size(); ++i) {
      //if the block was already decoded successfully, continue
      if (chunkVec[i].scanned) continue;
      cv::Mat tempResized;
      //resize the qrBlock
      cv::resize(chunkVec[i].m_image, tempResized, cv::Size(), dxdy, dxdy, cv::INTER_LINEAR );
      int width = tempResized.cols;
      int height = tempResized.rows;
      uchar *raw = (uchar *)tempResized.data;
      //pass the data to the zbar image
      zbar::Image image(width, height, "Y800", raw, width * height);
      //scan image
      int n = scanner.scan(image);
      //verify results
      if (n <= 0) {
        std::cout << "Can not read QR code " << i <<" at dx/dy value "<< dxdy << std::endl;
        // cv::imshow("Error", imgGray);
        // cv::waitKey(0);
      } else {
        chunkVec[i].scanned = true;
        for(zbar::Image::SymbolIterator symbol = image.symbol_begin(); symbol != image.symbol_end(); ++symbol) {
          chunkVec[i].message += symbol->get_data();
        }
        //separate the header from the body in the QRCode message
        chunkVec[i].separateHeader();
      }
    }
  }

  //combine the message
  int count = 0;
  int totalCount = 0;
  for (int i = 0; i < chunkVec.size(); i ++) {
    if(chunkVec[i].scanned) {
      if(totalCount < 1) {
        totalCount = chunkVec[i].getCount();
      }
      std::cout<<"adding "<<i<<std::endl;
      count ++;
    } else {
      cv::imshow(std::to_string(i), chunkVec[i].m_image);
      cv::waitKey(0);
    }
    //cv::imwrite(std::to_string(i)+".png", warped[i]);
  }

  if (count != totalCount) {
    std::cout<<"number of QR codes decoded does not match the count in QR header"<<std::endl;
    return 0;
  };
  //sort chunks by their index encoded in header
  std::sort(chunkVec.begin(), chunkVec.end());

  //combine data from body of each decoded QR code
  std::string finalMessage="";
  for (int i = 0; i < chunkVec.size(); i++){
    finalMessage += chunkVec[i].getBody();
  }

  //output final message
  std::ofstream outputFile("out/msg.mp3");
  outputFile << base64_decode(finalMessage);

  return 0;
}
