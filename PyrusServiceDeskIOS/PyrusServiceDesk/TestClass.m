//
//  TestClass.m
//  PyrusServiceDesk
//
//  Created by Галина  Муравьева on 02.03.2022.
//  Copyright © 2022  Галина Муравьева. All rights reserved.
//

#import "TestClass.h"
#import <UIKit/UIKit.h>
@implementation TestClass
+(NSString *)printInv {
    NSMutableString *str = [NSMutableString string];
    NSString *appearanceInvocationsKey = @"_appearanceInvocations";
    NSArray *appearanceInvocations = [[UINavigationBar appearance] valueForKey:appearanceInvocationsKey];
    [str appendString:[NSString stringWithFormat:@" UINavigationBar: %@", [TestClass printInvArray:appearanceInvocations]]];
    NSArray *appearanceInvocations2 = [[UIScrollView appearance] valueForKey:appearanceInvocationsKey];
    [str appendString:[NSString stringWithFormat:@", UIScrollView: %@", [TestClass printInvArray:appearanceInvocations2]]];
    NSArray *appearanceInvocations3 = [[UITableView appearance] valueForKey:appearanceInvocationsKey];
    [str appendString:[NSString stringWithFormat:@", UITableView: %@", [TestClass printInvArray:appearanceInvocations3]]];
    
    return str;
}
+(NSString *)printInvArray:(NSArray *)appearanceInvocations {
    NSMutableString *str = [NSMutableString string];
    [str appendString:@"{ "];
    for (NSInvocation *inv in appearanceInvocations) {
        [str appendString:inv.debugDescription];
        [str appendString:@", "];
    }
    [str appendString:@" }"];
    return str;
}
@end
