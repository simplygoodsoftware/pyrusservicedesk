#import "VorbisDecoder.h"

//#import "PSDTestProject2-Swift.h"
//#import "Helpy-Swift.h"

//#import <vorbis/vorbisfile.h>

#define MIN_OGGOPUS_CHANELS_COUNT 1
#define MAX_OGGOPUS_CHANELS_COUNT 2
@interface VorbisDecoder ()
{
@private
    int numberOfChanels;
    NSInteger _rate;
    CGFloat _totalTime;
}

@end

OggVorbis_File VorbisCreateEmptyFile(void) {
    return (OggVorbis_File){0};
}

@implementation VorbisDecoder

- (instancetype)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset{
    if (self != nil)
    {
        _vorbisFile = VorbisCreateEmptyFile();
        int err = ov_fopen([[url path] cStringUsingEncoding:NSUTF8StringEncoding], &_vorbisFile);
        vorbis_info *info = ov_info( &_vorbisFile, -1 );
        if (&_vorbisFile == NULL || err != OPUS_OK)
        {
            NSLog(@"ERROR OPEN FILE");
        }
        else if (ov_seekable(&_vorbisFile)){
            ov_pcm_seek(&_vorbisFile, psmOffset);
        }
        _rate = info->rate;
        if (_rate <= 0) {
            _rate = 44100;
        }
        numberOfChanels = info->channels;
        _totalTime = ov_time_total(&_vorbisFile, -1);
    }
    return self;
}

-(int)chanelsCount{
    return numberOfChanels;
}

-(int64_t)psmOffset{
    return 0;//ov_pcm_tell(&_vorbisFile);
}

- (BOOL)read:(AudioQueueBufferRef)pBuffer
{
    int nTotalBytesRead = 0;
    long nBytesRead = 0;
    int sec = 0;
    nBytesRead = ov_read(&_vorbisFile, pBuffer->mAudioData, pBuffer->mAudioDataBytesCapacity, 0, 2, 1, &sec);
    if(nBytesRead  > 0) {
        nTotalBytesRead += nBytesRead;
    }
    if(nTotalBytesRead == 0)
        return NO;
    if(nBytesRead < 0)
    {
        return NO;
    }
    pBuffer->mAudioDataByteSize = nTotalBytesRead;
    return YES;
    
}

- (CGFloat)getTotalTime {
    return _totalTime;
}

- (int64_t)getSampleRate {
    return _rate;
}

- (int64_t)getPcmTotal {
    return 0;
}

- (void)dealloc
{
//    ov_fr(vorbisFile);
}

@end
