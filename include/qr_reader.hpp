#ifndef QrReader_H
#define QrReader_H

#include <string>
#include "opencv2/core.hpp"


class QrReader {
public:
  bool read_qr_from_image(cv::Mat image); //true when done
  std::string combine_final_message();
  int clear_data();
private:
  std::vector<std::string> data;
};

#endif
