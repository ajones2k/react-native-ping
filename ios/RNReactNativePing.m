
#import "RNReactNativePing.h"
#import "GBPing.h"
#import "LHNetwork.h"
#import "LHDefinition.h"

@interface RNReactNativePing ()
@property (nonatomic,strong) dispatch_queue_t queue;
@end

@implementation RNReactNativePing


RCT_EXPORT_MODULE()
- (dispatch_queue_t)methodQueue
{
    if (!_queue) {
        _queue = dispatch_queue_create("com.pomato.React.RNReactNativePing", DISPATCH_QUEUE_SERIAL);
    }
    return _queue;
}

RCT_EXPORT_METHOD(
                  start:(NSString *)ipAddress
                  option:(NSDictionary *)option
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  ) {
    __block GBPing * ping = [[GBPing alloc] init];
    ping.timeout = 1.0;
    ping.pingPeriod = 0.9;
    ping.host = ipAddress;
    NSNumber *nsTimeout = option[@"timeout"];
    unsigned long long timeout = 1000.0;
    if (nsTimeout) {
        timeout = nsTimeout.unsignedLongLongValue;
        ping.timeout = timeout;
    }
    
    NSNumber *nsTtl = option[@"ttl"];
    if( nsTtl ) {
        ping.ttl = nsTtl.intValue;
        NSLog(@"ttl param = %@",nsTtl);
    }
    
    [ping setupWithBlock:^(BOOL success, NSError *_Nullable err)  {
        if (!success) {
            reject(@(err.code).stringValue,err.domain,err);
            return;
        }
        [ping startPingingWithBlock:^(GBPingSummary *summary) {
            if (!ping) {
                return;
            }
            NSString *rtt = [NSString stringWithFormat:@"%d",@(summary.rtt * 1000).intValue];
            NSString *ttl = [NSString stringWithFormat:@"%lu",summary.ttl];
            NSString *fromAddr = summary.host;
            NSString *matchAddr = [NSString stringWithFormat:@"%lu",summary.matchesAddress];
            resolve(@{
                @"rtt": rtt,
                @"ttl": ttl,
                @"fromAddr": fromAddr,
                @"matchesAddress": matchAddr
            });
            [ping stop];
            ping = nil;
        } fail:^(NSError *_Nonnull error) {
            if (!ping) {
                return;
            }
            reject(@(error.code).stringValue,error.domain,error);
            [ping stop];
            ping = nil;
        }];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(timeout * NSEC_PER_MSEC)), self->_queue, ^{
            if (!ping) {
                return;
            }
            ping = nil;
            DEFINE_NSError(timeoutError,PingUtil_Message_Timeout)
            reject(@(timeoutError.code).stringValue,timeoutError.domain,timeoutError);
        });
    }];
}
RCT_REMAP_METHOD(
                 getTrafficStats,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject
                 ) {
    // Prevent multiple calls from causing data confusion
    LHNetwork *instance = [[LHNetwork alloc]init];
    
    [instance checkNetworkflow];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC)), _queue, ^{
        [instance checkNetworkflow];
        
        NSString *receivedNetworkSpeed = instance.receivedNetworkSpeed;
        NSString *receivedNetworkTotal = instance.receivedNetworkTotal;
        NSString *sendNetworkSpeed = instance.sendNetworkSpeed;
        NSString *sendNetworkTotal = instance.sendNetworkTotal;
        resolve(@{
                  @"receivedNetworkSpeed": receivedNetworkSpeed,
                  @"receivedNetworkTotal": receivedNetworkTotal,
                  @"sendNetworkSpeed": sendNetworkSpeed,
                  @"sendNetworkTotal": sendNetworkTotal
                  });
    });
}
@end
