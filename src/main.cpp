#include <iostream>
#include <string>

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

#include "../include/qr_reader.hpp"

using namespace std;

int main(int argc, char* argv[]) {
  string file_path = "samples/qr_with_headers_01.png";
  cv::Mat image = cv::imread(file_path);
  QrReader qr;
  string data = qr.read_qr_from_image(image);
}