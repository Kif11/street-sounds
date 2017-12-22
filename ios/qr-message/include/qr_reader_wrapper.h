//
//  qr_reader_wrapper.h
//  qr-message
//
//  Created by Kirill Kovalewskiy  on 12/14/17.
//  Copyright © 2017 Kirill Kovalewskiy . All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface qr_reader_wrapper : NSObject

+ (bool) processQrImage:(UIImage *)isAllDecoded;
+ (NSString*) getFinalImage;
+ (int) clearData;
+ (UIImage* ) getDebugImage;

@end
