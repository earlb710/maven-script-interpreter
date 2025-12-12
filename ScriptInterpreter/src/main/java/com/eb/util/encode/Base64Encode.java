package com.eb.util.encode;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Earl Bosch
 */
public class Base64Encode implements Encode {

    @Override
    public byte[] decodeBytes(byte[] pStrBytes) {
        if (pStrBytes != null) {
            return decodeBase64(pStrBytes, 0, pStrBytes.length);
        } else {
            return new byte[0];
        }
    }

    @Override
    public byte[] encodeBytes(byte[] pStrBytes) {
        if (pStrBytes != null) {
            return encodeBase64(pStrBytes, 0, pStrBytes.length, BASE64_STANDARD_ENCODE_TABLE);
        } else {
            return new byte[0];
        }
    }

    public byte[] decodeBytesUrl(byte[] pStrBytes) {
        if (pStrBytes != null) {
            return decodeBase64(pStrBytes, 0, pStrBytes.length);
        } else {
            return new byte[0];
        }
    }

    public byte[] encodeBytesUrl(byte[] pStrBytes) {
        if (pStrBytes != null) {
            return encodeBase64(pStrBytes, 0, pStrBytes.length, BASE64_STANDARD_ENCODE_TABLE_URL);
        } else {
            return new byte[0];
        }
    }

    @Override
    public byte[] decodeString(String pStr) {
        try {
            return encodeBytes(pStr.getBytes("ASCII"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Base64Encode.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public byte[] encodeString(String pStr) {
        if (pStr != null) {
            try {
                return decodeBytes(pStr.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Base64Encode.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Simplified version derived form Base64.java
     *
     * This array is a lookup table that translates 6-bit positive integer index
     * values into their "Base64 Alphabet" equivalents as specified in Table 1
     * of RFC 2045.
     *
     * Thanks to "commons" project in ws.apache.org for this code.
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
     */
    private static final byte[] BASE64_DECODE_TABLE = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //17
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //34
        0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 0, 62, 62, 63, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4,
        5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, 0, 0, 0, 0, 63, 0, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };
    private static final byte[] BASE64_STANDARD_ENCODE_TABLE = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'
    };
    private static final byte[] BASE64_STANDARD_ENCODE_TABLE_URL = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '-', '_'
    };
    private static final int DEFAULT_BUFFER_RESIZE_ADDITION = 1024 * 8;
    private static final int MASK_8BITS = 0xff;
    private static final int MASK_6BITS = 0x3f;
    private static final int BITS_PER_ENCODED_BYTE = 6;
    private static final int BYTES_PER_UNENCODED_BLOCK = 3;
    private static final int BYTES_PER_ENCODED_BLOCK = 4;
    public final static byte BASE64_PAD = '=';

    public static byte[] encodeBase64(final byte[] inBuffer, int pStartPos, int pEndPos) {
        return encodeBase64(inBuffer, pStartPos, pEndPos, BASE64_STANDARD_ENCODE_TABLE);
    }

    public static byte[] encodeBase64Url(final byte[] inBuffer, int pStartPos, int pEndPos) {
        return encodeBase64(inBuffer, pStartPos, pEndPos, BASE64_STANDARD_ENCODE_TABLE_URL);
    }

    private static byte[] encodeBase64(final byte[] inBuffer, int pStartPos, int pEndPos, byte[] pEncodeTable) {
        byte[] encodeTable = pEncodeTable;
        int modulus = 0;
        int pos = 0;
        int bufferSize = (int) ((inBuffer.length + BYTES_PER_ENCODED_BLOCK - 1) * ((double) BYTES_PER_ENCODED_BLOCK / (double) BYTES_PER_UNENCODED_BLOCK));
        byte[] retBuffer = new byte[bufferSize];
        int ibitWorkArea = 0;
        //System.out.println("$ start = " + pStartPos + " : " + pEndPos + " : " + inBuffer.length);
        for (int i = pStartPos; i < pEndPos; i++) {
            modulus = (modulus + 1) % BYTES_PER_UNENCODED_BLOCK;
            int b = inBuffer[i];
            if (b < 0) {
                b += 256;
            }
            ibitWorkArea = (ibitWorkArea << 8) + b; //  BITS_PER_BYTE
            //System.out.println(modulus + " : " + ibitWorkArea + " : " + retBuffer);
            if (0 == modulus) { // 3 bytes = 24 bits = 4 * 6 bits to extract
                if (pos + BYTES_PER_ENCODED_BLOCK > bufferSize) {
                    retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
                    bufferSize = retBuffer.length;
                }
                retBuffer[pos++] = encodeTable[(ibitWorkArea >> 18) & MASK_6BITS];
                retBuffer[pos++] = encodeTable[(ibitWorkArea >> 12) & MASK_6BITS];
                retBuffer[pos++] = encodeTable[(ibitWorkArea >> 6) & MASK_6BITS];
                retBuffer[pos++] = encodeTable[ibitWorkArea & MASK_6BITS];
                //System.out.println("0$ " + pos + "/" + i + "=" + retBuffer[pos - 4] + " : " + retBuffer[pos - 3] + " : " + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
            }
        }
        if (2 == modulus) {
            if (pos + 2 > bufferSize) {
                retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
            }
            ibitWorkArea = (ibitWorkArea << 8);
            //System.out.println((char) encodeTable[((ibitWorkArea >> 18) & MASK_6BITS)] + " : " + (char) encodeTable[((ibitWorkArea >> 12) & MASK_6BITS)] + " : " + (char) encodeTable[((ibitWorkArea >> 6) & MASK_6BITS)]);
            retBuffer[pos++] = encodeTable[(ibitWorkArea >> 18) & MASK_6BITS];
            retBuffer[pos++] = encodeTable[(ibitWorkArea >> 12) & MASK_6BITS];
            retBuffer[pos++] = encodeTable[(ibitWorkArea >> 6) & MASK_6BITS];
            //retBuffer[pos++] = encodeTable[ibitWorkArea & MASK_6BITS];
            //System.out.println("2$ " + pos + "=" + retBuffer[pos - 4] + " : " + retBuffer[pos - 3] + " : " + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
        } else if (1 == modulus) {
            if (pos + 3 > bufferSize) {
                retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
            }
            ibitWorkArea = (ibitWorkArea << 16);
            retBuffer[pos++] = encodeTable[(ibitWorkArea >> 18) & MASK_6BITS];
            retBuffer[pos++] = encodeTable[(ibitWorkArea >> 12) & MASK_6BITS];
            //System.out.println("1$ " + pos + "=" + retBuffer[pos - 4] + " : " + retBuffer[pos - 3] + " : " + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
        }
        //System.out.println("$ lenght=" + pos);
        return resizeByteBuffer(pos, retBuffer);
    }

    public static byte[] decodeBase64(final byte[] inBuffer, int pStartPos, int pEndPos) {
        int modulus = 0;
        int pos = 0;
        //System.out.println("* start = " + pStartPos + " : " + pEndPos + " : " + inBuffer.length);
        int bufferSize = (int) ((inBuffer.length + BYTES_PER_UNENCODED_BLOCK - 1) * ((double) BYTES_PER_UNENCODED_BLOCK) / (double) BYTES_PER_ENCODED_BLOCK);
        byte[] retBuffer = new byte[bufferSize];
        int ibitWorkArea = 0, result = 0;
        for (int i = pStartPos; i < pEndPos; i++) {
            final int b64 = ((int) inBuffer[i]) & 0xFF;
            if (b64 < BASE64_DECODE_TABLE.length) {
                result = BASE64_DECODE_TABLE[b64];
            } else {
                result = 0;
            }
            modulus = (modulus + 1) % BYTES_PER_ENCODED_BLOCK;
            ibitWorkArea = (ibitWorkArea << BITS_PER_ENCODED_BYTE) + result;
            if (modulus == 0) {
                if (pos + BYTES_PER_UNENCODED_BLOCK > bufferSize) {
                    retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
                    bufferSize = retBuffer.length;
                }
                retBuffer[pos++] = (byte) ((ibitWorkArea >> 16) & MASK_8BITS);
                retBuffer[pos++] = (byte) ((ibitWorkArea >> 8) & MASK_8BITS);
                retBuffer[pos++] = (byte) (ibitWorkArea & MASK_8BITS);
                //System.out.println("0* " + pos + "/" + i + "=" + retBuffer[pos - 3] + " : " + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
            }
        }
        if (modulus == 3) {
            if (pos + 3 > bufferSize) {
                retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
            }
            ibitWorkArea = (ibitWorkArea << BITS_PER_ENCODED_BYTE);
            retBuffer[pos++] = (byte) ((ibitWorkArea >> 16) & MASK_8BITS);
            retBuffer[pos++] = (byte) ((ibitWorkArea >> 8) & MASK_8BITS);
            //retBuffer[pos++] = (byte) (ibitWorkArea & MASK_8BITS);
            //System.out.println("3* " + pos + "/" + "=" + retBuffer[pos - 3] + " : " + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
        } else if (modulus == 2) {
            if (pos + 2 > bufferSize) {
                retBuffer = resizeByteBuffer(bufferSize + DEFAULT_BUFFER_RESIZE_ADDITION, retBuffer);
            }
            ibitWorkArea = (ibitWorkArea << (BITS_PER_ENCODED_BYTE * 2));
            retBuffer[pos++] = (byte) ((ibitWorkArea >> 16) & MASK_8BITS);
            //retBuffer[pos++] = (byte) ((ibitWorkArea >> 8) & MASK_8BITS);
            //System.out.println("2* " + pos + "/" + "=" + retBuffer[pos - 2] + " : " + retBuffer[pos - 1]);
        } //else if (modulus == 1) {
        //ignore
        //}
        //System.out.println("* lenght=" + pos);
        return resizeByteBuffer(pos, retBuffer);
    }

    /**
     * Increases our buffer by the {@link #DEFAULT_BUFFER_RESIZE_ADDITION}.
     *
     * @param context the context to be used
     */
    private static byte[] resizeByteBuffer(final int pSize, final byte[] pBuffer) {
        byte[] retBuffer;
        if (pBuffer == null) {
            retBuffer = new byte[pSize];
        } else if (pSize == pBuffer.length) {
            retBuffer = pBuffer;
        } else {
            retBuffer = new byte[pSize];
            if (pSize > pBuffer.length) {
                System.arraycopy(pBuffer, 0, retBuffer, 0, pBuffer.length);
            } else {
                System.arraycopy(pBuffer, 0, retBuffer, 0, pSize);
            }
        }
        return retBuffer;
    }

}
