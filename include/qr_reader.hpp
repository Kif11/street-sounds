#ifndef QrReader_H
#define QrReader_H

#include <string>
#include "opencv2/core.hpp"


class QrReader {
public:
  std::string read_qr_from_image(cv::Mat image);
};

#endif