//
//  OpenCVWrapper.m
//  qr-message
//
//  Created by Kirill Kovalewskiy  on 12/12/17.
//  Copyright © 2017 Kirill Kovalewskiy . All rights reserved.
//

#import "person.h"
#import <iostream>
#import "OpenCVWrapper.h"

#import <opencv2/imgcodecs/ios.h>

// Here we can use C++ :)

@implementation OpenCVWrapper

- (void) isThisWorking {
    Person p;
    p.say_hello();
}

-(void) testOpenCV {
    Person p;
    p.test_open_cv();
}

+(NSString *) processUiImage:(UIImage *)image {
    cv::Mat imageMat;
    UIImageToMat(image, imageMat);
    return [NSString stringWithFormat:@"CV Version: %i, Image size: %i x %i", CV_MAJOR_VERSION, imageMat.cols, imageMat.rows];
}

@end
