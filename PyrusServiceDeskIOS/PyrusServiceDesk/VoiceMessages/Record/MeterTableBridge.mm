#import "MeterTableBridge.h"
#import "MeterTable.h"

@implementation MeterTableBridge{
    MeterTable* _meterTable;
}
+(MeterTable*)createMeterTable
{
    MeterTable* meterTable = NULL;
    meterTable = new MeterTable(kMinDBvalue);
    return meterTable;
}
- (instancetype)init
{
    self = [super init];
    if (self) {
        _meterTable = [MeterTableBridge createMeterTable];
    }
    return self;
}
-(float)valueAt:(float)inDecibels{
    return _meterTable->ValueAt(inDecibels);
}
- (void)dealloc
{
    delete _meterTable;
}
@end
