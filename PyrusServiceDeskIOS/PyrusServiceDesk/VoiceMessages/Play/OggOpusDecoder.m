#import "OggOpusDecoder.h"
#import "Helpy-Swift.h"
#define MIN_OGGOPUS_CHANELS_COUNT 1
#define MAX_OGGOPUS_CHANELS_COUNT 2
@interface OggOpusDecoder ()
{
@private
    int numberOfChanels;
}

@end

@implementation OggOpusDecoder

- (instancetype)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset{
    if (self != nil)
    {
        int openError = OPUS_OK;
        _oggFile = op_open_file([url.path UTF8String] , &openError);
        
        if (_oggFile == NULL || openError != OPUS_OK)
        {
            NSLog(@"ERROR OPEN FILE");
            return nil;
        }
        else if (op_seekable(_oggFile)){
            op_pcm_seek(_oggFile, psmOffset);
        }
        numberOfChanels = 1;// MAX(MIN_OGGOPUS_CHANELS_COUNT,MIN(MAX_OGGOPUS_CHANELS_COUNT,op_channel_count(_oggFile, -1)));
    }
    return self;
}
-(int)chanelsCount{
    return numberOfChanels;
}
-(int64_t)psmOffset{
    return op_pcm_tell(_oggFile);
}
- (BOOL)read:(AudioQueueBufferRef)pBuffer
{
    UInt32 nTotalBytesRead = 0;
    long nBytesRead = 0;
    UInt32 mBytesPerPacket = 2;
    do
    {
        if ((nTotalBytesRead + nBytesRead*mBytesPerPacket) > pBuffer->mAudioDataBytesCapacity) {
            break;
        }
        if(numberOfChanels == MAX_OGGOPUS_CHANELS_COUNT){
            nBytesRead = op_read_stereo(_oggFile, (opus_int16*)pBuffer->mAudioData + nTotalBytesRead/mBytesPerPacket,(int)(pBuffer->mAudioDataBytesCapacity - nTotalBytesRead/mBytesPerPacket));
        }
        else{
            nBytesRead = op_read(_oggFile,
                                 (opus_int16*)pBuffer->mAudioData + nTotalBytesRead/mBytesPerPacket,(int)(pBuffer->mAudioDataBytesCapacity - nTotalBytesRead/mBytesPerPacket), nil);
        }
        
        if(nBytesRead  > 0)
            nTotalBytesRead += nBytesRead*mBytesPerPacket*numberOfChanels;
        else {
            break;
        }
        
    } while(nTotalBytesRead < pBuffer->mAudioDataBytesCapacity);
    if(nTotalBytesRead == 0)
        return NO;
    if(nBytesRead < 0)
    {
        return NO;
    }
    pBuffer->mAudioDataByteSize = nTotalBytesRead;
    pBuffer->mPacketDescriptionCount = nTotalBytesRead/numberOfChanels;
    return YES;
    
}
- (void)dealloc
{
    op_free(_oggFile);
}

+(BOOL)canOpenFile:(NSURL*)url_ {
    return false;
}
@end
