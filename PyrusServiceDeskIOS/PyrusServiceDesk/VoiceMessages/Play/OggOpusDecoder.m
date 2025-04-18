#import "OggOpusDecoder.h"
#import "opusfile.h"
#define MIN_OGGOPUS_CHANELS_COUNT 1
#define MAX_OGGOPUS_CHANELS_COUNT 2
@interface OggOpusDecoder ()
{
@private
    OggOpusFile *oggFile;
    int numberOfChanels;
}

@end

@implementation OggOpusDecoder

- (instancetype)initWithUrl:(NSURL*)url offset:(int64_t)psmOffset{
    if (self != nil)
    {
        int openError = OPUS_OK;
        self->oggFile = op_open_file([url.path UTF8String] , &openError);
        if (self->oggFile == NULL || openError != OPUS_OK)
        {
            NSLog(@"ERROR OPEN FILE");
        }
        else if (op_seekable(self->oggFile)){
            op_pcm_seek(self->oggFile, psmOffset);
        }
        numberOfChanels = MAX(MIN_OGGOPUS_CHANELS_COUNT,MIN(MAX_OGGOPUS_CHANELS_COUNT,op_channel_count(oggFile, -1)));
    }
    return self;
}
-(int)chanelsCount{
    return numberOfChanels;
}
-(int64_t)psmOffset{
    return op_pcm_tell(self->oggFile);
}
- (BOOL)readBuffer:(AudioQueueBufferRef)pBuffer
{
    UInt32 nTotalBytesRead = 0;
    long nBytesRead = 0;
    UInt32 mBytesPerPacket = 2;
    do
    {
        
        
        if(numberOfChanels == MAX_OGGOPUS_CHANELS_COUNT){
            //returns the number of samples read per channel
            nBytesRead = op_read_stereo(oggFile, (opus_int16*)pBuffer->mAudioData + nTotalBytesRead/mBytesPerPacket,(int)(pBuffer->mAudioDataBytesCapacity - nTotalBytesRead/mBytesPerPacket));
        }
        else{
            // op_read returns number of samples read (per channel), and accepts number of samples which fit in the buffer, not number of bytes.
            nBytesRead = op_read(oggFile,
                                 (opus_int16*)pBuffer->mAudioData + nTotalBytesRead/mBytesPerPacket,(int)(pBuffer->mAudioDataBytesCapacity - nTotalBytesRead/mBytesPerPacket), nil);
        }
        
        if(nBytesRead  > 0)
            nTotalBytesRead += nBytesRead*mBytesPerPacket*numberOfChanels;
        else
            break;
        
    } while(nTotalBytesRead < pBuffer->mAudioDataBytesCapacity);
    if(nTotalBytesRead == 0)
        return NO;
    if(nBytesRead < 0)
    {
        return NO;
    }
    pBuffer->mAudioDataByteSize = nTotalBytesRead;
    pBuffer->mPacketDescriptionCount = nTotalBytesRead/mBytesPerPacket;
    return YES;
    
}
- (void)dealloc
{
    op_free(oggFile);
}
@end
