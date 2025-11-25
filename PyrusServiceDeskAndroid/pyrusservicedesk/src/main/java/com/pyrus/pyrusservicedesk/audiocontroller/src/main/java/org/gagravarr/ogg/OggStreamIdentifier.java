/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.ogg;

/**
 * Detector for identifying the kind of data stored in a given stream.
 * This is normally used on the first packet in a stream, to work out 
 *  the type, if recognised.
 * Note - the mime types and descriptions should be kept roughly in sync
 *  with those in Apache Tika
 *
 *  CHANGED: remove all unused fields and methods.
 */
public class OggStreamIdentifier {
    public static class OggStreamType {
        public final String mimetype;
        public final String description;
        public final Kind kind;
        public enum Kind { GENERAL, AUDIO, VIDEO, METADATA }

        protected OggStreamType(String mimetype, String description, Kind kind) {
            this.mimetype = mimetype;
            this.description = description;
            this.kind = kind;
        }
        public String toString() {
            return kind + " - " + description + " as " + mimetype;
        }
    }

   // Audio types
   public static final OggStreamType OPUS_AUDIO = new OggStreamType(
                                     "audio/opus", "Opus", OggStreamType.Kind.AUDIO);
   protected static final byte[] MAGIC_DAALA = new byte[8];
   static {
       MAGIC_DAALA[0] = (byte)0x80;
       IOUtils.putUTF8(MAGIC_DAALA, 1, "daala");
       // Remaining 2 bytes are all zero
   }

   protected static final byte[] MAGIC_KATE = new byte[8];
   static {
       MAGIC_KATE[0] = (byte)0x80;
       IOUtils.putUTF8(MAGIC_KATE, 1, "kate");
       // Remaining 3 bytes are all zero
   }

}
