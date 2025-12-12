package com.eb.util.encode;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eb.util.UtilConvert;

public final class SHA2 {

    private SHA2() { // static class
    }

    public static String getHexString(final byte[] pData) {
        char[] c = new char[pData.length * 2];
        for (int idx = 0; idx < pData.length; idx++) {
            c[idx * 2 + 1] = Character.forDigit(pData[idx] & 0x0F, 16);
            c[idx * 2] = Character.forDigit((pData[idx] & 0xF0) >> 4, 16);
        }
        return new String(c);
    }

    public static String getSHA2String(String pMessage) {
        byte[] d = null;
        try {
            d = digest512(pMessage.getBytes("UTF-16"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SHA2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getHexString(d);
    }

    public static String getSHA2String(char[] pMessage) {
        byte[] ret = null;
        try {
            byte[] msg = new String(pMessage).getBytes("UTF-16");
            ret = digest512(msg);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SHA2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getHexString(ret);
    }

    public static String getSHA2String_javaMessageDigest(String pMessage) throws NoSuchAlgorithmException {
        byte[] d = null;
        try {
            d = javaMessageDigest_SHA_512(pMessage.getBytes("UTF-16"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SHA2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getHexString(d);
    }

    public static String getSHA2String(byte[] pMessage) {
        byte[] d = null;
        d = digest512(pMessage);
        return UtilConvert.bytesToHex(d);
    }

    public static String getSHA2ShortString(String pMessage) {
        byte[] d = null;
        try {
            d = digest224(pMessage.getBytes("UTF-16"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SHA2.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*char[] c = new char[d.length];
        for (int idx = 0; idx < d.length; idx++) {
            //c[idx] = (char)d[idx];
            c[idx] = Character.forDigit(d[idx], 256);
        }*/
        return UtilConvert.bytesToHex(d);
    }

    /**
     * Class SHA2
     *
     * @param pDigestSize pDigestSize must be one of 224, 256, 384, 512
     * @param pMessage byte[]
     * @return byte[]
     *
     */
    public static byte[] digest(int pDigestSize, byte[] pMessage) throws NoSuchAlgorithmException {
        switch (pDigestSize) {
            case 224:
                return digest224(pMessage);
            case 256:
                return digest256(pMessage);
            case 384:
                return digest384(pMessage);
            case 512:
                return digest512(pMessage);
            default:
                throw new NoSuchAlgorithmException("algorithm must be one of SHA-224, SHA-256, SHA-384, or SHA-512");
        }
    }

    public static byte[] digest224(byte[] message) {
        byte[] hashed = new byte[28], block = new byte[64], padded = padMessage(message);
        int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};
        int[] H = {0xc1059ed8, 0x367cd507, 0x3070dd17, 0xf70e5939, 0xffc00b31, 0x68581511, 0x64f98fa7, 0xbefa4fa4};

        for (int i = 0; i < padded.length >> 6; i++) {
            int[] words = new int[64];
            int a = H[0], b = H[1], c = H[2], d = H[3], e = H[4], f = H[5], g = H[6], h = H[7], s0, s1, maj, t1, t2, ch;

            System.arraycopy(padded, 64 * i, block, 0, 64);
            for (int j = 0; j < 16; j++) {
                words[j] = 0;
                for (int k = 0; k < 4; k++) {
                    words[j] |= ((block[j * 4 + k] & 0x000000FF) << (24 - k * 8));
                }
            }

            for (int j = 16; j < 64; j++) {
                s0 = Integer.rotateRight(words[j - 15], 7) ^ Integer.rotateRight(words[j - 15], 18) ^ (words[j - 15] >>> 3);
                s1 = Integer.rotateRight(words[j - 2], 17) ^ Integer.rotateRight(words[j - 2], 19) ^ (words[j - 2] >>> 10);
                words[j] = words[j - 16] + s0 + words[j - 7] + s1;
            }

            for (int j = 0; j < 64; j++) {
                s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
                maj = (a & b) ^ (a & c) ^ (b & c);
                t2 = s0 + maj;
                s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
                ch = (e & f) ^ (~e & g);
                t1 = h + s1 + ch + K[j] + words[j];

                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }

            H[0] += a;
            H[1] += b;
            H[2] += c;
            H[3] += d;
            H[4] += e;
            H[5] += f;
            H[6] += g;
            H[7] += h;
        }

        for (int i = 0; i < 7; i++) {
            System.arraycopy(UtilConvert.intToBytes(H[i]), 0, hashed, 4 * i, 4);
        }

        return hashed;
    }

    public static byte[] digest256(byte[] message) {
        byte[] hashed = new byte[32], block = new byte[64], padded = padMessage(message);
        int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};
        int[] H = {0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19};

        for (int i = 0; i < padded.length >> 6; i++) {
            int[] words = new int[64];
            int a = H[0], b = H[1], c = H[2], d = H[3], e = H[4], f = H[5], g = H[6], h = H[7], s0, s1, maj, t1, t2, ch;

            System.arraycopy(padded, 64 * i, block, 0, 64);
            for (int j = 0; j < 16; j++) {
                words[j] = 0;
                for (int k = 0; k < 4; k++) {
                    words[j] |= ((block[j * 4 + k] & 0x000000FF) << (24 - k * 8));
                }
            }

            for (int j = 16; j < 64; j++) {
                s0 = Integer.rotateRight(words[j - 15], 7) ^ Integer.rotateRight(words[j - 15], 18) ^ (words[j - 15] >>> 3);
                s1 = Integer.rotateRight(words[j - 2], 17) ^ Integer.rotateRight(words[j - 2], 19) ^ (words[j - 2] >>> 10);
                words[j] = words[j - 16] + s0 + words[j - 7] + s1;
            }

            for (int j = 0; j < 64; j++) {
                s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
                maj = (a & b) ^ (a & c) ^ (b & c);
                t2 = s0 + maj;
                s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
                ch = (e & f) ^ (~e & g);
                t1 = h + s1 + ch + K[j] + words[j];

                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }

            H[0] += a;
            H[1] += b;
            H[2] += c;
            H[3] += d;
            H[4] += e;
            H[5] += f;
            H[6] += g;
            H[7] += h;
        }

        for (int i = 0; i < 8; i++) {
            System.arraycopy(UtilConvert.intToBytes(H[i]), 0, hashed, 4 * i, 4);
        }

        return hashed;
    }

    public static byte[] digest384(byte[] message) {
        byte[] hashed = new byte[48], block = new byte[128], padded = padMessage128(message);
        long[] K = {
            0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
            0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
            0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
            0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L, 0xc19bf174cf692694L,
            0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
            0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
            0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L,
            0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
            0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
            0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L, 0x92722c851482353bL,
            0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
            0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L,
            0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
            0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
            0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
            0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L, 0xc67178f2e372532bL,
            0xca273eceea26619cL, 0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
            0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
            0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
            0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L};
        long[] H = {0xcbbb9d5dc1059ed8L, 0x629a292a367cd507L, 0x9159015a3070dd17L, 0x152fecd8f70e5939L,
            0x67332667ffc00b31L, 0x8eb44a8768581511L, 0xdb0c2e0d64f98fa7L, 0x47b5481dbefa4fa4L};

        for (int i = 0; i < padded.length >> 7; i++) {
            long[] words = new long[80];
            long a = H[0], b = H[1], c = H[2], d = H[3], e = H[4], f = H[5], g = H[6], h = H[7], T1, T2;

            System.arraycopy(padded, 128 * i, block, 0, 128);
            for (int j = 0; j < 16; j++) {
                words[j] = 0;
                for (int k = 0; k < 8; k++) {
                    words[j] |= ((block[(j << 3) + k] & 0x00000000000000FFL) << (56 - (k << 3)));
                }
            }

            for (int j = 16; j < 80; j++) {
                words[j] = sigma1(words[j - 2]) + words[j - 7] + sigma0(words[j - 15]) + words[j - 16];
            }

            for (int j = 0; j < 80; j++) {
                T1 = h + ((e & f) ^ ((~e) & g)) + (Long.rotateRight(e, 14) ^ Long.rotateRight(e, 18) ^ Long.rotateRight(e, 41)) + words[j] + K[j];
                T2 = (Long.rotateRight(a, 28) ^ Long.rotateRight(a, 34) ^ Long.rotateRight(a, 39)) + ((a & b) ^ (a & c) ^ (b & c));
                h = g;
                g = f;
                f = e;
                e = d + T1;
                d = c;
                c = b;
                b = a;
                a = T1 + T2;

            }

            H[0] += a;
            H[1] += b;
            H[2] += c;
            H[3] += d;
            H[4] += e;
            H[5] += f;
            H[6] += g;
            H[7] += h;
        }

        for (int i = 0; i < 6; i++) {
            System.arraycopy(UtilConvert.longToBytes(H[i]), 0, hashed, 8 * i, 8);
        }

        return hashed;
    }

    public static byte[] digest512(final byte[] pMessage) {
        byte[] message = pMessage;
        final byte[] retHashed = new byte[64];
        final long[] words = new long[80];
        final long[] K512 = {
            0x428A2F98D728AE22L, 0x7137449123EF65CDL, 0xB5C0FBCFEC4D3B2FL,
            0xE9B5DBA58189DBBCL, 0x3956C25BF348B538L, 0x59F111F1B605D019L,
            0x923F82A4AF194F9BL, 0xAB1C5ED5DA6D8118L, 0xD807AA98A3030242L,
            0x12835B0145706FBEL, 0x243185BE4EE4B28CL, 0x550C7DC3D5FFB4E2L,
            0x72BE5D74F27B896FL, 0x80DEB1FE3B1696B1L, 0x9BDC06A725C71235L,
            0xC19BF174CF692694L, 0xE49B69C19EF14AD2L, 0xEFBE4786384F25E3L,
            0x0FC19DC68B8CD5B5L, 0x240CA1CC77AC9C65L, 0x2DE92C6F592B0275L,
            0x4A7484AA6EA6E483L, 0x5CB0A9DCBD41FBD4L, 0x76F988DA831153B5L,
            0x983E5152EE66DFABL, 0xA831C66D2DB43210L, 0xB00327C898FB213FL,
            0xBF597FC7BEEF0EE4L, 0xC6E00BF33DA88FC2L, 0xD5A79147930AA725L,
            0x06CA6351E003826FL, 0x142929670A0E6E70L, 0x27B70A8546D22FFCL,
            0x2E1B21385C26C926L, 0x4D2C6DFC5AC42AEDL, 0x53380D139D95B3DFL,
            0x650A73548BAF63DEL, 0x766A0ABB3C77B2A8L, 0x81C2C92E47EDAEE6L,
            0x92722C851482353BL, 0xA2BFE8A14CF10364L, 0xA81A664BBC423001L,
            0xC24B8B70D0F89791L, 0xC76C51A30654BE30L, 0xD192E819D6EF5218L,
            0xD69906245565A910L, 0xF40E35855771202AL, 0x106AA07032BBD1B8L,
            0x19A4C116B8D2D0C8L, 0x1E376C085141AB53L, 0x2748774CDF8EEB99L,
            0x34B0BCB5E19B48A8L, 0x391C0CB3C5C95A63L, 0x4ED8AA4AE3418ACBL,
            0x5B9CCA4F7763E373L, 0x682E6FF3D6B2B8A3L, 0x748F82EE5DEFB2FCL,
            0x78A5636F43172F60L, 0x84C87814A1F0AB72L, 0x8CC702081A6439ECL,
            0x90BEFFFA23631E28L, 0xA4506CEBDE82BDE9L, 0xBEF9A3F7B2C67915L,
            0xC67178F2E372532BL, 0xCA273ECEEA26619CL, 0xD186B8C721C0C207L,
            0xEADA7DD6CDE0EB1EL, 0xF57D4F7FEE6ED178L, 0x06F067AA72176FBAL,
            0x0A637DC5A2C898A6L, 0x113F9804BEF90DAEL, 0x1B710B35131C471BL,
            0x28DB77F523047D84L, 0x32CAAB7B40C72493L, 0x3C9EBE0A15C9BEBCL,
            0x431D67C49C100D4CL, 0x4CC5D4BECB3E42B6L, 0x597F299CFC657E2AL,
            0x5FCB6FAB3AD6FAECL, 0x6C44198C4A475817L
        };

        final long[] ht = {
            0x6A09E667F3BCC908L, 0xBB67AE8584CAA73BL,
            0x3C6EF372FE94F82BL, 0xA54FF53A5F1D36F1L,
            0x510E527FADE682D1L, 0x9B05688C2B3E6C1FL,
            0x1F83D9ABFB41BD6BL, 0x5BE0CD19137E2179L
        };
        long t1, t2;
        final long[] v = {
            0, 0
        };
        final byte[] padMessage = paddedPart(message);
        //final int blockCount = pmessage.length >> 7;

        int blockCount = ((message.length + padMessage.length) >> 7) - 1;
        for (int i = 0; i <= blockCount; i++) {
            if (i == blockCount && padMessage.length > 0) {
                message = padMessage;
                i = 0;
                blockCount = 0;
            }

            v[0] = ht[0];
            v[1] = ht[1];
            long c = ht[2];
            long d = ht[3];
            long e = ht[4];
            long f = ht[5];
            long g = ht[6];
            long h = ht[7];

            for (int j = 0; j < 16; j++) {
                words[j] = 0;
                for (int k = 0; k < 8; k++) {
                    words[j] |= ((message[(i << 7) + (j << 3) + k] & 0xFFL) << (56 - (k << 3)));
                }
                t1 = h + ((e & f) ^ ((~e) & g)) + (rotateRight(e, 14) ^ rotateRight(e, 18) ^ rotateRight(e, 41)) + words[j] + K512[j];
                t2 = (rotateRight(v[0], 28) ^ rotateRight(v[0], 34) ^ rotateRight(v[0], 39)) + ((v[0] & v[1]) ^ (v[0] & c) ^ (v[1] & c));
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = v[1];
                v[1] = v[0];
                v[0] = t1 + t2;
            }

            for (int j = 16; j < 80; j++) {
                words[j] = sigma1(words[j - 2]) + words[j - 7] + sigma0(words[j - 15]) + words[j - 16];
                t1 = h + ((e & f) ^ ((~e) & g)) + (rotateRight(e, 14) ^ rotateRight(e, 18) ^ rotateRight(e, 41)) + words[j] + K512[j];
                t2 = (rotateRight(v[0], 28) ^ rotateRight(v[0], 34) ^ rotateRight(v[0], 39)) + ((v[0] & v[1]) ^ (v[0] & c) ^ (v[1] & c));
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = v[1];
                v[1] = v[0];
                v[0] = t1 + t2;
            }

            ht[0] += v[0];
            ht[1] += v[1];
            ht[2] += c;
            ht[3] += d;
            ht[4] += e;
            ht[5] += f;
            ht[6] += g;
            ht[7] += h;
        }

        for (int i = 0; i < 8; i++) {
            int shift = (56 - (i << 3));
            retHashed[i] = (byte) (ht[0] >>> shift);
            retHashed[8 + i] = (byte) (ht[1] >>> shift);
            retHashed[16 + i] = (byte) (ht[2] >>> shift);
            retHashed[24 + i] = (byte) (ht[3] >>> shift);
            retHashed[32 + i] = (byte) (ht[4] >>> shift);
            retHashed[40 + i] = (byte) (ht[5] >>> shift);
            retHashed[48 + i] = (byte) (ht[6] >>> shift);
            retHashed[56 + i] = (byte) (ht[7] >>> shift);
        }
        return retHashed;
    }

    private static long sigma0(final long val) {
        return rotateRight(val, 1) ^ rotateRight(val, 8) ^ (val >>> 7);
    }

    private static long sigma1(final long val) {
        return rotateRight(val, 19) ^ rotateRight(val, 61) ^ (val >>> 6);
    }

    private static long rotateRight(final long i, final int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /*private static long sum0(long a) {
        return Long.rotateRight(a, 28) ^ Long.rotateRight(a, 34) ^ Long.rotateRight(a, 39);
    }

    private static long sum1(long e) {
        return Long.rotateRight(e, 14) ^ Long.rotateRight(e, 18) ^ Long.rotateRight(e, 41);
    }

    private static long ch(long e, long f, long g) {
        return (e & f) ^ ((~e) & g);
    }

    private static long maj(long a, long b, long c) {
        return (a & b) ^ (a & c) ^ (b & c);
    }*/
    private static byte[] padMessage(final byte[] data) {
        int origLength = data.length;
        int tailLength = origLength & 63;
        int padLength = 0;
        if ((64 - tailLength >= 9)) {
            padLength = 64 - tailLength;
        } else {
            padLength = 128 - tailLength;
        }
        byte[] thePad = new byte[padLength];
        thePad[0] = (byte) 0x80;
        long lengthInBits = ((long) origLength) * 8;
        for (int i = 0; i < 8; i++) {
            thePad[thePad.length - 1 - i] = (byte) ((lengthInBits >>> (8 * i)) & 0xFF);
        }

        byte[] output = new byte[origLength + padLength];

        System.arraycopy(data, 0, output, 0, origLength);
        System.arraycopy(thePad, 0, output, origLength, thePad.length);
        return output;
    }

    private static byte[] padMessage128(byte[] data) {
        final int padLength = 128 - (data.length & 127);
        final long lengthInBits = ((long) data.length) << 3;

        byte[] output = new byte[data.length + padLength];
        System.arraycopy(data, 0, output, 0, data.length);
        output[data.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            output[output.length - 1 - i] = (byte) (lengthInBits >> (8 * i));
        }

        return output;
    }

    private static byte[] paddedPart(byte[] data) {
        final int dataLength = (data.length & 127);
        if (dataLength > 0 || data.length == 0) {
            final long lengthInBits = ((long) data.length) << 3;

            byte[] output = new byte[128];
            System.arraycopy(data, data.length - dataLength, output, 0, dataLength);
            output[dataLength] = (byte) 0x80;
            for (int i = 0; i < 8; i++) {
                output[output.length - 1 - i] = (byte) (lengthInBits >> (8 * i));
            }
            return output;
        } else {
            return new byte[0];
        }
    }

    public static byte[] javaMessageDigest_SHA_512(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] bytesSHA = md.digest(message);
        return bytesSHA;
    }

}
