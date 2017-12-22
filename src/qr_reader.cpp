#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

#include "zbar.h"
#include "../include/base_64.h"
#include "../include/qr_chunk.hpp"
#include "../include/qr_reader.hpp"

#include <iostream>
#include <string>
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


bool QrReader::read_qr_from_image(cv::Mat image) {

  this->image = image;

  int total_img_area = image.rows * image.cols;
  int erode_mat_coeff = image.rows / 121;

  //initialize helper cv::Mat 's
  cv::Mat gray, thresh, threshErode, otsu_tmp;

  //convert to grayscale
  cv::cvtColor(image, gray, CV_RGBA2GRAY);

  //find the best thresholding value using otsu's algorithm
  //we want to only consider the area that contains the qr code paper
  cv::Rect myROI(0.2*image.cols, 0.2*image.rows, 0.5*image.cols, 0.5*image.rows);
  int otsu_val = cv::threshold(gray(myROI), otsu_tmp, 0, 255, cv::THRESH_BINARY|cv::THRESH_OTSU);
  cv::threshold(gray, thresh, otsu_val, 255, cv::THRESH_BINARY);

  //erode
  cv::Mat M = cv::Mat::ones(erode_mat_coeff, erode_mat_coeff, CV_8U);
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
    if (poly.rows == 4 && area < 0.53*total_img_area && area > 0.002*total_img_area) {
    //if (poly.rows == 4 && area > 0.002*total_img_area) {

      std::vector<cv::Point2f> pts = orderPoints(poly);
      qrBlocks.push_back(pts);
      // cv::Scalar color = cv::Scalar( 255,0,0 );
      // drawContours( image, contours, i, color, 2, 8, hierarchy, 0, cv::Point() );
    }
  }

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
    warpPerspective(image, imgWarped, P, cv::Size(maxWidth, maxHeight) );
    //convert img to gray before feeding into zbar::imageScanner
    cv::cvtColor(imgWarped, imgGray, CV_RGBA2GRAY);
    cv::copyMakeBorder(imgGray, imgGray, 10, 10, 10, 10, cv::BORDER_CONSTANT, 255);

    QrChunk::QrChunk chunk(imgGray);
    chunk.setOriginalCoords(pts);
    chunkVec.push_back(chunk);
  }

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
        // std::cout << "[-] Can not read QR code " << i <<" at dx/dy value "<< dxdy << std::endl;
        // cv::imshow("Error", imgGray);
        // cv::waitKey(0);
      } else {
        chunkVec[i].scanned = true;
          std::string messageTemp = "";
        for(zbar::Image::SymbolIterator symbol = image.symbol_begin(); symbol != image.symbol_end(); ++symbol) {
          messageTemp += symbol->get_data();
        }
        if ( !chunkVec[i].setData(messageTemp) ) {
          chunkVec[i].scanned = false;
        }
      }
    }
  }

  int totalCount = 0;
  //combine the message
  for ( int i = 0; i < chunkVec.size(); i ++ ) {
    if(chunkVec[i].scanned) {
      if( this->data.size() == 0 ) {
        totalCount = chunkVec[i].getCount();
        this->data.resize(totalCount);
      }
      // std::cout << "[+] Image decoded: " << i << std::endl;
      this->data[chunkVec[i].getIndex()] = chunkVec[i].getBody();
      cv::rectangle(this->image, chunkVec[i].m_pts[0], chunkVec[i].m_pts[2], cv::Scalar(0, 255, 0, 255), 60);

    } else {
      //show the image that failed...
      // std::cout << "[!] Image failed: " << i << std::endl;
    }
    //cv::imwrite(std::to_string(i)+".png", warped[i]);
  }

  if ( this->data.size() == 0 ) {
    // std::cout << "[!] No qr codes detected " << std::endl;
    return false;
  }

  bool allScanned = true;

  for ( int i = 0; i < this->data.size(); i++ ) {
    if ( this->data[i].size() < 1 ) {
      std::cout << "[!] Still looking for qrcode : " << i << std::endl;
      allScanned = false;
    }
  }
  return allScanned;
}

std::string QrReader::combine_final_message() {
  // combine data from body of each decoded QR code
  std::string finalMessage="";
  for ( auto&x : this->data ){
    finalMessage += x;
  }
  return finalMessage;
}

cv::Mat QrReader::get_debug_image() {return this->image;}

int QrReader::clear_data() {
  this->data.clear();
  return 0;
}
