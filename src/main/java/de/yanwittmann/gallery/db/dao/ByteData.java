package de.yanwittmann.gallery.db.dao;

import java.util.Arrays;

public class ByteData {
    private byte[] data;

    public ByteData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public ByteData(String data) {
        this.data = toByteArray(data);
    }

    public static byte[] toByteArray(String hex) {
        String normalizedHex = hex.startsWith("\\x") ? hex.substring(2) : hex;
        int len = normalizedHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(normalizedHex.charAt(i), 16) << 4)
                                  + Character.digit(normalizedHex.charAt(i+1), 16));
        }
        return data;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteData) {
            return Arrays.equals(data, ((ByteData) obj).data);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "ByteData{" +
               "data=" + Arrays.toString(data) +
               '}';
    }
}
