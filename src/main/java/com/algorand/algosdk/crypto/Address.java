package com.algorand.algosdk.crypto;


import com.algorand.algosdk.util.Digester;
import com.algorand.algosdk.util.Encoder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.codec.binary.Base32;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Address represents a serializable 32-byte length Algorand address.
 */
public class Address implements Serializable {
    /**
     * The length of an address. Equal to the size of a SHA256 checksum.
     */
    public static final int LEN_BYTES = 32;

    // the underlying bytes
    private final byte[] bytes = new byte[LEN_BYTES];
    // the length of checksum to append
    private static final int CHECKSUM_LEN_BYTES = 4;
    // expected length of base32-encoded checksum-appended addresses
    private static final int EXPECTED_STR_ENCODED_LEN = 58;

    /**
     * Create a new address from a byte array.
     * @param bytes array of 32 bytes
     */
    @JsonCreator
    public Address(final byte[] bytes) {
        if (bytes == null) {
            return;
        }
        if (bytes.length != LEN_BYTES) {
            throw new IllegalArgumentException(String.format("Given address length is not %s", LEN_BYTES));
        }
        System.arraycopy(bytes, 0, this.bytes, 0, LEN_BYTES);
    }

    // default values for serializer to ignore
    public Address() {
    }

    /**
     * Get the underlying bytes wrapped by this Address.
     * @return 32 byte array
     */
    @JsonValue
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Create a new address from an encoded string, (encoded by encodeAsString)
     * @param encodedAddr
     */
    public Address(final String encodedAddr) throws NoSuchAlgorithmException {
        Objects.requireNonNull(encodedAddr, "address must not be null");
        // interpret as base32
        Base32 codec = new Base32();
        final byte[] checksumAddr = codec.decode(encodedAddr); // may expect padding
        // sanity check length
        if (checksumAddr.length != LEN_BYTES + CHECKSUM_LEN_BYTES) {
            throw new IllegalArgumentException("Input string is an invalid address. Wrong length");
        }
        // split into checksum
        final byte[] checksum = Arrays.copyOfRange(checksumAddr, LEN_BYTES, checksumAddr.length);
        final byte[] addr = Arrays.copyOf(checksumAddr, LEN_BYTES); // truncates

        // compute expected checksum
        final byte[] hashedAddr = Digester.digest(Arrays.copyOf(addr, LEN_BYTES));
        final byte[] expectedChecksum = Arrays.copyOfRange(hashedAddr, LEN_BYTES - CHECKSUM_LEN_BYTES, hashedAddr.length);

        // compare
        if (Arrays.equals(checksum, expectedChecksum)) {
            System.arraycopy(addr, 0, this.bytes, 0, LEN_BYTES);
        } else {
            throw new IllegalArgumentException("Input checksum did not validate");
        }
    }

    /**
     * encodeAsString converts the address to a human-readable representation, with
     * a 4-byte checksum appended at the end, using SHA256. Note that string representations
     * of addresses generated by different SDKs may not be compatible.
     * @return
     */
    public String encodeAsString() throws NoSuchAlgorithmException {
        // compute sha512/256 checksum
        final byte[] hashedAddr = Digester.digest(Arrays.copyOf(bytes, LEN_BYTES));

        // take the last 4 bytes, and append to addr
        final byte[] checksum = Arrays.copyOfRange(hashedAddr, LEN_BYTES - CHECKSUM_LEN_BYTES, hashedAddr.length);
        byte[] checksumAddr = Arrays.copyOf(this.bytes, this.bytes.length + CHECKSUM_LEN_BYTES);
        System.arraycopy(checksum, 0, checksumAddr, bytes.length, CHECKSUM_LEN_BYTES);

        // encodeToMsgPack addr+checksum as base32 and return. Strip padding.
        String res = Encoder.encodeToBase32StripPad(checksumAddr);
        if (res.length() != EXPECTED_STR_ENCODED_LEN) {
            throw new RuntimeException("unexpected address length " + res.length());
        }
        return res;
    }

    @Override
    public String toString() {
        try {
            return this.encodeAsString();
        } catch (NoSuchAlgorithmException e) {
            // encoding should always succeed when provider properly set up
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Address && Arrays.equals(this.bytes, ((Address)obj).bytes)) {
            return true;
        } else {
            return false;
        }
    }
}
