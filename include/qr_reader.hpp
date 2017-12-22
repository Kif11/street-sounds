#ifndef QrReader_H
#define QrReader_H

#include <string>
#include "opencv2/core.hpp"


class QrReader {
public:
  bool read_qr_from_image(cv::Mat image); //true when done
  std::string combine_final_message();
  cv::Mat get_debug_image();
private:
  std::vector<std::string> data;
  cv::Mat image;
};

#endif
