#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioUnit/AudioUnit.h>
#import <AudioToolbox/AudioToolbox.h>


@interface OggOpusDecoder: NSObject
- (id)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset;
- (BOOL)readBuffer:(AudioQueueBufferRef)buffer;
-(int64_t)psmOffset;
-(int)chanelsCount;
@end
