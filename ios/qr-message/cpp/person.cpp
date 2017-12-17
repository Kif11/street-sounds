#include <iostream>
#include "person.h"
#import "opencv2/core.hpp"
#import "opencv2/imgproc.hpp"
#import "zbar.h"

void Person::say_hello() {
  std::cout << "Hello world!" << std::endl;
};

void Person::test_open_cv() {
    cv::Mat mat;
    std::cout << "Mat size: " << mat.size() << std::endl;
    
    zbar::ImageScanner scanner;
    scanner.set_config(zbar::ZBAR_NONE, zbar::ZBAR_CFG_ENABLE, 1);
};
