#ifndef QrChunk_H    // To make sure you don't declare the function more than once by including the header multiple times.
#define QrChunk_H

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

#include "zbar.h"
#include "../include/base_64.h"

#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>

/*
 * Chunk of file data with costom header to be encoded to QR code
 */
class QrChunk {

  public:

    cv::Mat m_image;
    std::vector<cv::Point2f> m_pts;
    bool scanned = false;
    std::string message = "";

    // CONSTRUCTORS
    QrChunk() {}
    QrChunk(std::string data);
    QrChunk(cv::Mat image);

    // GETTERS
    std::string getHeader();
    std::string getBody();
    std::string getData();
    int getCount () const;
    int getIndex () const;

    //SETTERS
    void setBody(std::string data);
    void setImage(cv::Mat image);
    void setOriginalCoords(std::vector<cv::Point2f> pts);

    //COMPARATOR
    bool operator<( const QrChunk& val ) const;

    //MEMBER FUNCTIONS
    bool setData ( std::string _message );

    // void setHeader(int index, int total) {
    //   char header[m_raw_header_size];
    //   snprintf(header, sizeof(header), "%02d%02d", index, total);
    //   m_header = base64_encode((unsigned char*)header, m_raw_header_size);
    // }

  private:

    int m_raw_header_size = 5;
    int m_header_size = 8;
    std::string m_header; // in base64 format
    std::string m_body; // in base64 format
    int index = -1;
    int count = -1;
};


#endif
