#import "VorbisDecoder.h"
#import "Helpy-Swift.h"

#import <vorbis/vorbisfile.h>

#define MIN_OGGOPUS_CHANELS_COUNT 1
#define MAX_OGGOPUS_CHANELS_COUNT 2
@interface VorbisDecoder ()
{
@private
    int numberOfChanels;
}

@end

@implementation VorbisDecoder

- (instancetype)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset{
    if (self != nil)
    {
        _vorbisFile = [VorbisDecoder getFile];
        int err = ov_fopen([[url path] cStringUsingEncoding:NSUTF8StringEncoding], &_vorbisFile);
        vorbis_info *info = ov_info( &_vorbisFile, -1 );
        if (&_vorbisFile == NULL || err != OPUS_OK)
        {
            NSLog(@"ERROR OPEN FILE");
        }
        else if (ov_seekable(&_vorbisFile)){
            ov_pcm_seek(&_vorbisFile, psmOffset);
        }
        numberOfChanels = info->channels;
    }
    return self;
}

-(int)chanelsCount{
    return numberOfChanels;
}

-(int64_t)psmOffset{
    return ov_pcm_tell(&_vorbisFile);
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
- (void)dealloc
{
//    ov_fr(vorbisFile);
}

@end
