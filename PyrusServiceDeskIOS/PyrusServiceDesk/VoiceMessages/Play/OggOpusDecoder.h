#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioUnit/AudioUnit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "opusfile.h"

@protocol AudioDecoderProtocol;

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
