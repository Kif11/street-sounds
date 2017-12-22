#include <iostream>
#include <string>
#include <fstream>

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

#include "../include/qr_reader.hpp"
#include "../include/base_64.h"

using namespace std;

int main(int argc, char* argv[]) {

  QrReader qr;

  bool qrCodesDetected = false;
  cv::Mat curFrame;
  //cv::VideoCapture stream(0);
  // cv::VideoCapture stream("samples/test.mov");
  //
  // if(!stream.isOpened()) {
  //   std::cout << "cannot open camera" << std::endl;
  // }
  std::vector <std::string> images;
  images.push_back("samples/test1.jpg");
  images.push_back("samples/test2.jpg");
  int i = 0;

  while(!qrCodesDetected && i<images.size()) {
    curFrame = cv::imread(images[i]);
    //stream.read(curFrame);
    qrCodesDetected = qr.read_qr_from_image(curFrame);
    cv::imshow("window", curFrame);
    i++;
  }

  std::string finalMessage = qr.combine_final_message();
  //std::cout << finalMessage << std::endl;
  // output final message
  std::ofstream outputFile("out/msg.mp3");
  outputFile << base64_decode(finalMessage);
  return 0;
}
