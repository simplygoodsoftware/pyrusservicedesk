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
package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.opus;

import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.ogg.IOUtils;
import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.ogg.OggStreamPacket;

/**
 * Parent of all Opus packets
 */
public interface OpusPacket extends OggStreamPacket {
   String MAGIC_HEADER_STR = "OpusHead";
   String MAGIC_TAGS_STR = "OpusTags";
   byte[] MAGIC_HEADER_BYTES = IOUtils.toUTF8Bytes(MAGIC_HEADER_STR);
   byte[] MAGIC_TAGS_BYTES   = IOUtils.toUTF8Bytes(MAGIC_TAGS_STR);
}