#import <Foundation/Foundation.h>
//#import <vorbis/vorbisfile.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioUnit/AudioUnit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "opusfile.h"

NS_ASSUME_NONNULL_BEGIN
@protocol AudioDecoderProtocol;

@interface VorbisDecoder : NSObject<AudioDecoderProtocol>
//@property(assign) OggVorbis_File vorbisFile;

- (instancetype)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset;

@end

//OggVorbis_File VorbisCreateEmptyFile(void);

NS_ASSUME_NONNULL_END
