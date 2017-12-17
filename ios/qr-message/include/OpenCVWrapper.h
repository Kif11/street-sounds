//
//  OpenCVWrapper.h
//  qr-message
//
//  Created by Kirill Kovalewskiy  on 12/12/17.
//  Copyright © 2017 Kirill Kovalewskiy . All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface OpenCVWrapper : NSObject

- (void) isThisWorking;
- (void) testOpenCV;
+ (NSString *) processUiImage:(UIImage *)image;

@end
