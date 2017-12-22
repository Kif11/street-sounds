//
//  qr_reader_wrapper.m
//  qr-message
//
//  Created by Kirill Kovalewskiy  on 12/14/17.
//  Copyright © 2017 Kirill Kovalewskiy . All rights reserved.
//

#import "qr_reader_wrapper.h"
#import "qr_reader.hpp"
#import <opencv2/imgcodecs/ios.h>
#import <string>

@implementation qr_reader_wrapper

QrReader qr;

+(bool) processQrImage:(UIImage *)image {
  cv::Mat imageMat;
  UIImageToMat(image, imageMat);
  return qr.read_qr_from_image(imageMat);
}

+(NSString*) getFinalImage {
  std::string data = qr.combine_final_message();
  return [NSString stringWithCString:data.c_str() encoding:[NSString defaultCStringEncoding]];
}

+(int) clearData {
  return qr.clear_data();
}

@end
