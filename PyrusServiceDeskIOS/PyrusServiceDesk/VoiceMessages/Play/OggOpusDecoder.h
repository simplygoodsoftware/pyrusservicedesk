#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioUnit/AudioUnit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "opusfile.h"

@protocol AudioDecoderProtocol <NSObject>

- (int)chanelsCount;
- (int64_t)psmOffset;
- (BOOL)read:(AudioQueueBufferRef)buffer;
- (int64_t)getPcmTotal;
- (CGFloat)getTotalTime;
- (int64_t)getSampleRate;

@end

typedef enum {
    opus,
    vorbis
} OggType;

@interface OggOpusDecoder: NSObject<AudioDecoderProtocol>
@property(assign) OggOpusFile *oggFile;

- (id)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset;
//- (BOOL)readBuffer:(AudioQueueBufferRef)buffer;
//-(int64_t)psmOffset;
//-(int)chanelsCount;
@end
