/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.wrapper;

import io.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.utils.bytebuf.ByteBufAbstract;
import io.github.retrooper.packetevents.manager.player.ClientVersion;
import io.github.retrooper.packetevents.utils.wrapper.PacketWrapperUtils;
import net.minecraft.util.com.google.common.base.Charsets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;


public class PacketWrapper {
    protected final ClientVersion version;
    protected final ByteBufAbstract byteBuf;

    public PacketWrapper(ClientVersion version, ByteBufAbstract byteBuf) {
        this.version = version;
        this.byteBuf = byteBuf;
    }

    public ClientVersion getClientVersion() {
        return version;
    }

    public ByteBufAbstract getByteBuf() {
        return byteBuf;
    }

    public byte readByte() {
        return byteBuf.readByte();
    }

    public int readInt() {
        return byteBuf.readInt();
    }

    public int readVarInt() {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = byteBuf.readByte();
            i |= (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    public String readString(ClientVersion version) {
        return readString(version, 32767);
    }

    public String readString(ServerVersion version) {
        return readString(version, 32767);
    }

    public String readString(ClientVersion version, int maxLen) {
        return readString(version.getProtocolVersion(), maxLen);
    }

    public String readString(ServerVersion version, int maxLen) {
        return readString(version.getProtocolVersion(), maxLen);
    }

    public String readString(int protocolVersion, int maxLen) {
        //1.12 and higher
        if (protocolVersion >= 335) {
            return readStringModern(maxLen);
        } else {
            return readStringLegacy(maxLen);
        }
    }

    private String readStringLegacy(int i) {
        int j = readVarInt();
        if (j > i * 4) {
            throw new RuntimeException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + (i * 4) + ")");
        } else if (j < 0) {
            throw new RuntimeException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            ByteBufAbstract bb = byteBuf.readBytes(j);
            byte[] bytes;
            if (bb.hasArray()) {
                bytes = bb.array();
            }
            else {
                bytes = new byte[bb.readableBytes()];
                bb.getBytes(bb.readerIndex(), bytes);
            }
            String s = new String(bytes);
            if (s.length() > i) {
                throw new RuntimeException("The received string length is longer than maximum allowed (" + j + " > " + i + ")");
            }
            return s;
        }
    }

    private String readStringModern(int i) {
        int j = readVarInt();
        if (j > i * 4) {
            throw new RuntimeException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ")");
        } else if (j < 0) {
            throw new RuntimeException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String s = byteBuf.toString(byteBuf.readerIndex(), j, StandardCharsets.UTF_8);
            byteBuf.readerIndex(byteBuf.readerIndex() + j);
            if (s.length() > i) {
                throw new RuntimeException("The received string length is longer than maximum allowed (" + j + " > " + i + ")");
            } else {
                return s;
            }
        }
    }

    public int readUnsignedShort() {
        return byteBuf.readUnsignedShort();
    }

    public short readShort() {
        return byteBuf.readShort();
    }

    public long readLong() {
        return byteBuf.readLong();
    }

    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    public byte[] readByteArray(int length) {
        byte[] ret = new byte[length];
        byteBuf.readBytes(ret);
        return ret;
    }

    public UUID readUUID() {
        long mostSigBits = readLong();
        long leastSigBits = readLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    public static PacketWrapper createUniversalPacketWrapper(ByteBufAbstract byteBuf) {
        return new PacketWrapper(ClientVersion.UNKNOWN, byteBuf);
    }
}