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

+(NSString *) processQrImage:(UIImage *)image {
    cv::Mat imageMat;
    UIImageToMat(image, imageMat);
    
    QrReader qr;
    std::string data = qr.read_qr_from_image(imageMat);
    
    return [NSString stringWithCString:data.c_str() encoding:[NSString defaultCStringEncoding]];
}

@end
