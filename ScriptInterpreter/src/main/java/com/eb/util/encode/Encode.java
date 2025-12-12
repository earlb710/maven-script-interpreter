package com.eb.util.encode;

/**
 *
 * @author Earl Bosch
 */
public interface Encode {

    public byte[] decodeString(String pStr);

    public byte[] encodeString(String pStr);

    public byte[] decodeBytes(byte[] pStr);

    public byte[] encodeBytes(byte[] pStr);

}
