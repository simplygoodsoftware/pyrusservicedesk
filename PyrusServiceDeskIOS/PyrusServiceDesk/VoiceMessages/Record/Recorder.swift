//  Changed:
//  kAudioQueueProperty_CurrentLevelMeterDB to kAudioQueueProperty_CurrentLevelMeter in private func samplePower() to get current volume

//  Recorder.swift
//  OpusRecorder
//
//  Created by Omair Baskanderi on 2017-03-06.
//
//  This class was extracted from:
//  watson-developer-cloud/swift-sdk SpeechToTextRecorder.swift
//
//  source:
//  https://github.com/watson-developer-cloud/swift-sdk/blob/master/Source/SpeechToTextV1/SpeechToTextRecorder.swift
//
//  Copyright IBM Corporation 2016
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import Foundation
import AudioToolbox
import AVFoundation

let OpusAudioSampleRate: Double = 16000
internal class Recorder {
    
    // This implementation closely follows Apple's "Audio Queue Services Programming Guide".
    // See the guide for more information about audio queues and recording.
    
    internal var onMicrophoneData: ((Data) -> Void)?                 // callback to handle pcm buffer
   // internal var onPowerData: ((Float32) -> Void)?                   // callback for average dB power
    internal let session = AVAudioSession.sharedInstance()           // session for recording permission
    internal var isRecording = false                                 // state of recording
    internal private(set) var format = AudioStreamBasicDescription() // audio data format specification
    
    private var queue: AudioQueueRef? = nil                          // opaque reference to an audio queue
    
    private let callback: AudioQueueInputCallback = {
        userData, queue, bufferRef, startTimeRef, numPackets, packetDescriptions in
        
        // parse `userData` as `Recorder`
        guard let userData = userData else { return }
        let audioRecorder = Unmanaged<Recorder>.fromOpaque(userData).takeUnretainedValue()
        
        // dereference pointers
        let buffer = bufferRef.pointee
        let startTime = startTimeRef.pointee
        
        // calculate number of packets
        var numPackets = numPackets
        if numPackets == 0 && audioRecorder.format.mBytesPerPacket != 0 {
            numPackets = buffer.mAudioDataByteSize / audioRecorder.format.mBytesPerPacket
        }
        
        // work with pcm data in an Autorelease Pool to make sure it is released in a timely manner
        autoreleasepool {
            
            // execute callback with audio data
            let pcm = Data(bytes: buffer.mAudioData, count: Int(buffer.mAudioDataByteSize))
            audioRecorder.onMicrophoneData?(pcm)
        }
        
        // return early if recording is stopped
        guard audioRecorder.isRecording else {
            return
        }
        
        // enqueue buffer
        if let queue = audioRecorder.queue {
            AudioQueueEnqueueBuffer(queue, bufferRef, 0, nil)
        }
    }
    
    internal init() {
        // define audio format
        var formatFlags = AudioFormatFlags()
        formatFlags |= kLinearPCMFormatFlagIsSignedInteger
        formatFlags |= kLinearPCMFormatFlagIsPacked
        format = AudioStreamBasicDescription(
            mSampleRate: OpusAudioSampleRate,
            mFormatID: kAudioFormatLinearPCM,
            mFormatFlags: formatFlags,
            mBytesPerPacket: UInt32(1 * MemoryLayout<Int16>.stride),
            mFramesPerPacket: 1,
            mBytesPerFrame: UInt32(1 * MemoryLayout<Int16>.stride),
            mChannelsPerFrame: 1,
            mBitsPerChannel: 16,
            mReserved: 0
        )
    }
    
    private func prepareToRecord() {
        // create recording queue
        let pointer = UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque())
        AudioQueueNewInput(&format, callback, pointer, nil, nil, 0, &queue)
        
        // ensure queue was set
        guard let queue = queue else {
            return
        }
        
        // update audio format
        var formatSize = UInt32(MemoryLayout<AudioStreamBasicDescription>.stride)
        AudioQueueGetProperty(queue, kAudioQueueProperty_StreamDescription, &format, &formatSize)
        
        // allocate and enqueue buffers
        let numBuffers = 5
        let bufferSize = Recorder.deriveBufferSize(seconds: 0.5, audioQueue: queue, streamDesc: format)
        for _ in 0..<numBuffers {
            let bufferRef = UnsafeMutablePointer<AudioQueueBufferRef?>.allocate(capacity: 1)
            AudioQueueAllocateBuffer(queue, bufferSize, bufferRef)
            if let buffer = bufferRef.pointee {
                AudioQueueEnqueueBuffer(queue, buffer, 0, nil)
            }
        }
        
        // enable metering
        var metering: UInt32 = 1
        let meteringSize = UInt32(MemoryLayout<UInt32>.stride)
        let meteringProperty = kAudioQueueProperty_EnableLevelMetering
        AudioQueueSetProperty(queue, meteringProperty, &metering, meteringSize)
        
    }
    internal func startRecording() throws {
        guard !isRecording else { return }
        self.prepareToRecord()
        self.isRecording = true
        guard let queue = queue else { return }
        AudioQueueStart(queue, nil)
    }
    
    internal func stopRecording() throws {
        guard isRecording else { return }
        guard let queue = queue else { return print("can not stop recording") }
        isRecording = false
        AudioQueueStop(queue, true)
        AudioQueueDispose(queue, false)
        onMicrophoneData = nil
    }
    
    static func deriveBufferSize(seconds: Float64, audioQueue: AudioQueueRef?, streamDesc: AudioStreamBasicDescription) -> UInt32 {
        guard let queue = audioQueue else { return 0 }
        let maxBufferSize = UInt32(0x50000)
        var maxPacketSize = streamDesc.mBytesPerPacket
        if maxPacketSize == 0 {
            var maxVBRPacketSize = UInt32(MemoryLayout<UInt32>.stride)
            AudioQueueGetProperty(
                queue,
                kAudioQueueProperty_MaximumOutputPacketSize,
                &maxPacketSize,
                &maxVBRPacketSize
            )
        }
        
        let numBytesForTime = UInt32(streamDesc.mSampleRate * Float64(maxPacketSize) * seconds)
        let bufferSize = (numBytesForTime < maxBufferSize ? numBytesForTime : maxBufferSize)
        return bufferSize
    }
    private var meters = [AudioQueueLevelMeterState(mAveragePower: 0, mPeakPower: 0)]
    private var meterTable: MeterTableBridge? = MeterTableBridge()
    
    func getLevelMeters(_ count: Int, in range: ClosedRange<CGFloat>) -> [CGFloat] {
        guard let queue = queue else { return [CGFloat]() }
        var metersSize = UInt32(meters.count * MemoryLayout<AudioQueueLevelMeterState>.stride)
        let meteringProperty = kAudioQueueProperty_CurrentLevelMeterDB
        let meterStatus = AudioQueueGetProperty(queue, meteringProperty, &meters, &metersSize)
        guard meterStatus == 0 else { return [CGFloat]() }
        let avP = meterTable?.value(at: meters[0].mAveragePower) ?? 0.0
        let count = count
        let maxValue = CGFloat(avP) * range.upperBound
        
        let arr: [CGFloat] = (0..<count).map { _ in range.lowerBound + CGFloat.random(in: 0 ... maxValue) }
        return arr
       
    }
    deinit {
        meterTable = nil
    }
}
